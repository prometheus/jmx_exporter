/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.negative;

import static io.prometheus.jmx.test.support.http.HttpResponse.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.paramixel.api.Context.withInstance;

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.environment.NetworkSupport;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Scope;
import org.paramixel.api.action.Sequence;
import org.paramixel.api.action.Step;
import org.testcontainers.containers.Network;

public class EnvVarNotSetTest {

    private final JmxExporterTestEnvironment environment;
    private Network network;

    private EnvVarNotSetTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    @Paramixel.Disabled
    public static Action factory() throws Throwable {
        var environments = JmxExporterTestEnvironment.createTestEnvironments(EnvVarNotSetTest.class).stream()
                .filter(e -> e.getJmxExporterMode() == JmxExporterMode.JavaAgent)
                .collect(Collectors.toList());

        return Each.parallel(
                        EnvVarNotSetTest.class.getName(),
                        environments,
                        env -> Instance.builder(env.name(), () -> new EnvVarNotSetTest(env))
                                .body(Scope.builder("scenario")
                                        .before(Step.of(
                                                "setUp()",
                                                withInstance(EnvVarNotSetTest.class, EnvVarNotSetTest::setUp)))
                                        .body(Sequence.builder("tests")
                                                .child(Step.of(
                                                        "testContainerBehavior()",
                                                        withInstance(
                                                                EnvVarNotSetTest.class,
                                                                EnvVarNotSetTest::testContainerBehavior))))
                                        .after(Step.of(
                                                "tearDown()",
                                                withInstance(EnvVarNotSetTest.class, EnvVarNotSetTest::tearDown)))))
                .build();
    }

    public void setUp() throws Throwable {
        network = NetworkSupport.create();
        environment.initialize(network);
    }

    public void testContainerBehavior() throws IOException {
        String url = environment.getUrl(JmxExporterPath.HEALTHY);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);

        String metricsUrl = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse metricsResponse = HttpClient.sendRequest(metricsUrl);
        assertThat(metricsResponse.statusCode()).isEqualTo(200);

        Collection<Metric> metrics = MetricsParser.parseCollection(metricsResponse);

        String buildInfoName = environment.getJmxExporterMode().getBuildInfoName();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_exporter_build_info")
                .withLabel("name", buildInfoName)
                .withValue(1d)
                .isPresent();
    }

    public void tearDown() throws Throwable {
        environment.close();
        NetworkSupport.close(network);
    }
}
