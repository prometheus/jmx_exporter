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

package io.prometheus.jmx.test.support;

import org.antublue.test.engine.api.Argument;

public class TestArguments implements Argument<TestArguments> {

    private final String name;
    private final String dockerImageName;
    private final JmxExporterMode jmxExporterMode;

    private TestArguments(String name, String dockerImageName, JmxExporterMode jmxExporterMode) {
        this.name = name;
        this.dockerImageName = dockerImageName;
        this.jmxExporterMode = jmxExporterMode;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TestArguments getPayload() {
        return this;
    }

    public String getDockerImageName() {
        return dockerImageName;
    }

    public JmxExporterMode getJmxExporterMode() {
        return jmxExporterMode;
    }

    @Override
    public String toString() {
        return String.format(
                "TestArgument{name=[%s],dockerImageName=[%s],mode=[%s]}",
                name, dockerImageName, jmxExporterMode);
    }

    public static TestArguments of(
            String name, String dockerImageName, JmxExporterMode jmxExporterMode) {
        return new TestArguments(name, dockerImageName, jmxExporterMode);
    }
}
