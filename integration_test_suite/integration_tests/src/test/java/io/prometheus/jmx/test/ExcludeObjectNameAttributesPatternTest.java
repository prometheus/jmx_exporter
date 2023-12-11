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

package io.prometheus.jmx.test;

import static org.assertj.core.api.Assertions.*;

import io.prometheus.jmx.test.support.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.antublue.test.engine.api.TestEngine;

public class ExcludeObjectNameAttributesPatternTest extends BaseTest implements ContentConsumer {

    @TestEngine.Test
    public void testHealthy() {
        RequestResponseAssertions.assertThatResponseForRequest(
                        new HealthyRequest(testState.httpClient()))
                .isSuperset(HealthyResponse.RESULT_200);
    }

    @TestEngine.Test
    public void testMetrics() {
        RequestResponseAssertions.assertThatResponseForRequest(
                        new MetricsRequest(testState.httpClient()))
                .isSuperset(MetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        RequestResponseAssertions.assertThatResponseForRequest(
                        new OpenMetricsRequest(testState.httpClient()))
                .isSuperset(OpenMetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        RequestResponseAssertions.assertThatResponseForRequest(
                        new PrometheusMetricsRequest(testState.httpClient()))
                .isSuperset(PrometheusMetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @Override
    public void accept(String content) {
        Collection<Metric> metricCollection = MetricsParser.parse(content);

        Set<String> excludeAttributeNameSet = new HashSet<>();
        excludeAttributeNameSet.add("_ClassPath");
        excludeAttributeNameSet.add("_SystemProperties");
        excludeAttributeNameSet.add("_UsageThreshold");
        excludeAttributeNameSet.add("_UsageThresholdCount");
        /*
         * Assert that we don't have any metrics that start with ...
         *
         * name = java_lang*
         */
        Set<String> validAttributesFound = new HashSet<>();
        metricCollection.forEach(
                metric -> {
                    String name = metric.getName();
                    // test global exclusion filter
                    if (name.contains("_ObjectName")) fail("metric found: " + name);
                    // test exclusion by object name patterns
                    if (name.contains("java_lang")) {
                        for (String attributeName : excludeAttributeNameSet) {
                            if (name.endsWith(attributeName)) {
                                fail("metric found: " + name);
                            }
                        }
                        // test exclusion accuracy
                        if (name.contains("_Valid")) {
                            // Valid should not exist inside MemoryPool
                            if (name.contains("MemoryPool")) {
                                fail("metric found: " + name);
                            } else { // Valid should exist inside other branches
                                validAttributesFound.add(name);
                            }
                        }
                    }
                });
        assertThat(!validAttributesFound.isEmpty()).as("metric should exist").isTrue();
    }
}
