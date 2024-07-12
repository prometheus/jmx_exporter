/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.common;

import io.prometheus.jmx.test.support.JavaDockerImages;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpHealthyRequest;
import io.prometheus.jmx.test.support.http.HttpMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpOpenMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusProtobufMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.http.HttpResponseAssertions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.containers.Network;

public abstract class AbstractExporterTest implements Consumer<HttpResponse> {

    protected Network network;

    @TestEngine.Argument public ExporterTestEnvironment exporterTestEnvironment;

    /**
     * Method to get the Stream of test environments
     *
     * @return the Stream of test environments
     */
    @TestEngine.ArgumentSupplier
    public static Stream<ExporterTestEnvironment> arguments() {
        Collection<ExporterTestEnvironment> collection = new ArrayList<>();

        JavaDockerImages.names()
                .forEach(
                        dockerImageName -> {
                            for (JmxExporterMode jmxExporterMode : JmxExporterMode.values()) {
                                collection.add(
                                        new ExporterTestEnvironment(
                                                dockerImageName, jmxExporterMode));
                            }
                        });

        return collection.stream();
    }

    @TestEngine.Prepare
    public final void prepare() {
        // Create a Network and get the id to force the network creation
        network = Network.newNetwork();
        network.getId();
    }

    @TestEngine.BeforeAll
    public void beforeAll() {
        exporterTestEnvironment.initialize(getClass(), network);
    }

    @TestEngine.Test
    public void testHealthy() {
        new HttpHealthyRequest()
                .send(exporterTestEnvironment.getHttpClient())
                .accept(HttpResponseAssertions::assertHttpHealthyResponse);
    }

    @TestEngine.Test
    public void testMetrics() {
        new HttpMetricsRequest().send(exporterTestEnvironment.getHttpClient()).accept(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        new HttpOpenMetricsRequest().send(exporterTestEnvironment.getHttpClient()).accept(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        new HttpPrometheusMetricsRequest()
                .send(exporterTestEnvironment.getHttpClient())
                .accept(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusProtobufFormat() {
        new HttpPrometheusProtobufMetricsRequest()
                .send(exporterTestEnvironment.getHttpClient())
                .accept(this);
    }

    @TestEngine.AfterAll
    public void afterAll() {
        if (exporterTestEnvironment != null) {
            exporterTestEnvironment.destroy();
            exporterTestEnvironment = null;
        }
    }

    @TestEngine.Conclude
    public final void conclude() {
        if (network != null) {
            network.close();
            network = null;
        }
    }

    @Override
    public abstract void accept(HttpResponse httpResponse);
}
