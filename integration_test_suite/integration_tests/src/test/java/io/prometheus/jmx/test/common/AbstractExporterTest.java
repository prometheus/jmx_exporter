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
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.antublue.verifyica.api.ArgumentContext;
import org.antublue.verifyica.api.ClassContext;
import org.antublue.verifyica.api.Verifyica;
import org.testcontainers.containers.Network;

public abstract class AbstractExporterTest
        implements BiConsumer<ExporterTestEnvironment, HttpResponse> {

    public static final String NETWORK = "network";

    /**
     * Method to get the Stream of test environments
     *
     * @return the Stream of test environments
     */
    @Verifyica.ArgumentSupplier
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

    @Verifyica.Prepare
    public static void prepare(ClassContext classContext) {
        // Create a Network and get the id to force the network creation
        Network network = Network.newNetwork();
        network.getId();

        classContext.getStore().put(NETWORK, network);
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Network network = argumentContext.getClassContext().getStore().get(NETWORK, Network.class);
        Class<?> testClass = argumentContext.getClassContext().getTestClass();

        argumentContext
                .getTestArgument(ExporterTestEnvironment.class)
                .getPayload()
                .initialize(testClass, network);
    }

    @Verifyica.Test
    public void testHealthy(ArgumentContext argumentContext) {
        ExporterTestEnvironment exporterTestEnvironment =
                argumentContext.getTestArgument(ExporterTestEnvironment.class).getPayload();

        new HttpHealthyRequest()
                .send(exporterTestEnvironment.getHttpClient())
                .accept(HttpResponseAssertions::assertHttpHealthyResponse);
    }

    @Verifyica.Test
    public void testMetrics(ArgumentContext argumentContext) {
        ExporterTestEnvironment exporterTestEnvironment =
                argumentContext.getTestArgument(ExporterTestEnvironment.class).getPayload();

        accept(
                exporterTestEnvironment,
                new HttpMetricsRequest().send(exporterTestEnvironment.getHttpClient()));
    }

    @Verifyica.Test
    public void testMetricsOpenMetricsFormat(ArgumentContext argumentContext) {
        ExporterTestEnvironment exporterTestEnvironment =
                argumentContext.getTestArgument(ExporterTestEnvironment.class).getPayload();

        accept(
                exporterTestEnvironment,
                new HttpOpenMetricsRequest().send(exporterTestEnvironment.getHttpClient()));
    }

    @Verifyica.Test
    public void testMetricsPrometheusFormat(ArgumentContext argumentContext) {
        ExporterTestEnvironment exporterTestEnvironment =
                argumentContext.getTestArgument(ExporterTestEnvironment.class).getPayload();

        accept(
                exporterTestEnvironment,
                new HttpPrometheusMetricsRequest().send(exporterTestEnvironment.getHttpClient()));
    }

    @Verifyica.Test
    public void testMetricsPrometheusProtobufFormat(ArgumentContext argumentContext) {
        ExporterTestEnvironment exporterTestEnvironment =
                argumentContext.getTestArgument(ExporterTestEnvironment.class).getPayload();

        accept(
                exporterTestEnvironment,
                new HttpPrometheusProtobufMetricsRequest()
                        .send(exporterTestEnvironment.getHttpClient()));
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) {
        Optional.ofNullable(argumentContext.getTestArgument(ExporterTestEnvironment.class))
                .ifPresent(
                        exporterTestEnvironmentArgument ->
                                exporterTestEnvironmentArgument.getPayload().destroy());
    }

    @Verifyica.Conclude
    public static void conclude(ClassContext classContext) {
        Optional.ofNullable(classContext.getStore().remove(NETWORK, Network.class))
                .ifPresent(Network::close);
    }

    @Override
    public abstract void accept(
            ExporterTestEnvironment exporterTestEnvironment, HttpResponse httpResponse);
}
