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

package io.prometheus.jmx.logger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class LevelTest {

    @Test
    public void testLevelValues() {
        Level[] levels = Level.values();
        assertThat(levels).hasSize(4);
        assertThat(levels).containsExactly(Level.TRACE, Level.INFO, Level.WARN, Level.ERROR);
    }

    @Test
    public void testLevelValueOf() {
        assertThat(Level.valueOf("TRACE")).isEqualTo(Level.TRACE);
        assertThat(Level.valueOf("INFO")).isEqualTo(Level.INFO);
        assertThat(Level.valueOf("WARN")).isEqualTo(Level.WARN);
        assertThat(Level.valueOf("ERROR")).isEqualTo(Level.ERROR);
    }
}
