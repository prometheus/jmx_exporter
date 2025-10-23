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

package io.prometheus.jmx.common.util;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

public class PreconditionTest {

    @Test
    public void testNotNull1() {
        Precondition.notNull(new Object());
    }

    @Test
    public void testNotNull2() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Precondition.notNull(null));
    }

    @Test
    public void testNotNull1NotEmpty1() {
        Precondition.notNullOrEmpty("test");
    }

    @Test
    public void testNotNull1NotEmpty2() {
        Precondition.notNullOrEmpty(" test ");
    }

    @Test
    public void testNotNull1NotEmpty3() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Precondition.notNullOrEmpty(" "));
    }

    @Test
    public void testNotNull1NotEmpty4() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Precondition.notNullOrEmpty("   "));
    }

    @Test
    public void testNotNull1NotEmpty5() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Precondition.notNullOrEmpty("\t\r\n"));
    }

    @Test
    public void testNotNull1NotEmpty6() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Precondition.notNullOrEmpty("\t"));
    }

    @Test
    public void testIsGreaterThanOrEqualTo1() {
        Precondition.isGreaterThanOrEqualTo(1, 1);
    }

    @Test
    public void testIsGreaterThanOrEqualTo2() {
        Precondition.isGreaterThanOrEqualTo(2, 1);
    }

    @Test
    public void testIsGreaterThanOrEqualTo3() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Precondition.isGreaterThanOrEqualTo(0, 1));
    }
}
