/*
 * Copyright (C) 2018-2023 The Prometheus jmx_exporter Authors
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
package io.prometheus.jmx;

import static org.junit.Assert.assertEquals;

import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

public class BuildInfoCollectorTest {

    private CollectorRegistry registry = new CollectorRegistry();

    @Before
    public void setUp() {
        new BuildInfoCollector().register(registry);
    }

    @Test
    public void testBuildInfo() {
        String version = this.getClass().getPackage().getImplementationVersion();
        String name = this.getClass().getPackage().getImplementationTitle();

        assertEquals(
                1L,
                registry.getSampleValue(
                        "jmx_exporter_build_info",
                        new String[] {"version", "name"},
                        new String[] {
                            version != null ? version : "unknown", name != null ? name : "unknown"
                        }),
                .0000001);
    }
}
