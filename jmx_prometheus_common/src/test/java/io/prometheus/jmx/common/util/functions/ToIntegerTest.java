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

public class ToIntegerTest {

    @Test
    public void testApplyValidIntegerString() {
        ToInteger toInteger = new ToInteger(() -> new RuntimeException("should not be thrown"));
        assertThat(toInteger.apply("42")).isEqualTo(42);
        assertThat(toInteger.apply("0")).isEqualTo(0);
        assertThat(toInteger.apply("-123")).isEqualTo(-123);
        assertThat(toInteger.apply("2147483647")).isEqualTo(2147483647);
    }

    @Test
    public void testApplyIntegerInput() {
        ToInteger toInteger = new ToInteger(() -> new RuntimeException("should not be thrown"));
        assertThat(toInteger.apply(42)).isEqualTo(42);
    }

    @Test
    public void testApplyNumericString() {
        ToInteger toInteger = new ToInteger(() -> new RuntimeException("should not be thrown"));
        assertThat(toInteger.apply("123")).isEqualTo(123);
    }

    @Test
    public void testApplyNullThrowsIllegalArgumentException() {
        ToInteger toInteger = new ToInteger(() -> new RuntimeException("should not be thrown"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> toInteger.apply(null));
    }

    @Test
    public void testApplyInvalidStringThrowsSupplierException() {
        ToInteger toInteger = new ToInteger(() -> new UnsupportedOperationException("parse failed"));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> toInteger.apply("not a number"));
    }

    @Test
    public void testApplyEmptyStringThrowsSupplierException() {
        ToInteger toInteger = new ToInteger(() -> new UnsupportedOperationException("parse failed"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> toInteger.apply(""));
    }

    @Test
    public void testConstructorNullSupplier() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ToInteger(null));
    }
}
