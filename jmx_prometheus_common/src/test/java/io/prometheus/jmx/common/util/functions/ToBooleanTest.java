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

package io.prometheus.jmx.common.util.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

public class ToBooleanTest {

    @Test
    public void testApplyBooleanInput() {
        ToBoolean toBoolean = new ToBoolean(() -> new RuntimeException("should not be thrown"));
        assertThat(toBoolean.apply(Boolean.TRUE)).isTrue();
        assertThat(toBoolean.apply(Boolean.FALSE)).isFalse();
    }

    @Test
    public void testApplyTrueString() {
        ToBoolean toBoolean = new ToBoolean(() -> new RuntimeException("should not be thrown"));
        assertThat(toBoolean.apply("true")).isTrue();
        assertThat(toBoolean.apply("TRUE")).isTrue();
        assertThat(toBoolean.apply("True")).isTrue();
    }

    @Test
    public void testApplyFalseString() {
        ToBoolean toBoolean = new ToBoolean(() -> new RuntimeException("should not be thrown"));
        assertThat(toBoolean.apply("false")).isFalse();
        assertThat(toBoolean.apply("FALSE")).isFalse();
        assertThat(toBoolean.apply("False")).isFalse();
    }

    @Test
    public void testApplyNullThrowsIllegalArgumentException() {
        ToBoolean toBoolean = new ToBoolean(() -> new RuntimeException("should not be thrown"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> toBoolean.apply(null));
    }

    @Test
    public void testApplyInvalidStringReturnsFalse() {
        ToBoolean toBoolean = new ToBoolean(() -> new RuntimeException("should not be thrown"));
        assertThat(toBoolean.apply("not a boolean")).isFalse();
    }

    @Test
    public void testApplyEmptyStringReturnsFalse() {
        ToBoolean toBoolean = new ToBoolean(() -> new RuntimeException("should not be thrown"));
        assertThat(toBoolean.apply("")).isFalse();
    }

    @Test
    public void testConstructorNullSupplier() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ToBoolean(null));
    }
}
