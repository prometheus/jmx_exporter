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

import static io.prometheus.jmx.test.support.http.HttpResponseAssertions.assertHttpMetricsResponse;
import static org.assertj.core.api.Assertions.fail;

import io.prometheus.jmx.test.common.AbstractExporterTest;
import io.prometheus.jmx.test.common.ExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class ExcludeObjectNameAttributesTest extends AbstractExporterTest
        implements BiConsumer<ExporterTestEnvironment, HttpResponse> {

    @Override
    public void accept(ExporterTestEnvironment exporterTestEnvironment, HttpResponse httpResponse) {
        assertHttpMetricsResponse(httpResponse);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        Set<String> excludeAttributeNameSet = new HashSet<>();
        excludeAttributeNameSet.add("_ClassPath");
        excludeAttributeNameSet.add("_SystemProperties");

        Set<String> excludeJavaLangMemoryAttributeSet = new HashSet<>();
        excludeJavaLangMemoryAttributeSet.add("NonHeapMemoryUsage");
        excludeJavaLangMemoryAttributeSet.add("Verbose");
        excludeJavaLangMemoryAttributeSet.add("ObjectPendingFinalizationCount");

        /*
         * Assert that we don't have any metrics that start with ...
         *
         * name = java_lang*
         * attribute = _ClassPath
         * attribute = __SystemProperties
         *
         * ... or...
         *
         * name = java_lang_Memory
         * attribute = _Verbose
         */
        metrics.forEach(
                metric -> {
                    String name = metric.name();
                    if (name.equals("java_lang_Memory")) {
                        for (String attributeName : excludeJavaLangMemoryAttributeSet) {
                            if (name.equals(attributeName)) {
                                fail("metric [" + metric + "] found");
                            }
                        }
                    } else {
                        for (String attributeName : excludeAttributeNameSet) {
                            if (name.contains(attributeName)) {
                                fail("metric [" + metric + "] found");
                            }
                        }
                    }
                });
    }
}
