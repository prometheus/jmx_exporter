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

public class ToStringTest {

    @Test
    public void testApplyStringInput() {
        ToString toString = new ToString(() -> new RuntimeException("should not be thrown"));
        String result = toString.apply("hello world");
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    public void testApplyObjectWithToString() {
        ToString toString = new ToString(() -> new RuntimeException("should not be thrown"));
        Integer integer = 42;
        String result = toString.apply(integer);
        assertThat(result).isEqualTo("42");
    }

    @Test
    public void testApplyCustomObject() {
        ToString toString = new ToString(() -> new RuntimeException("should not be thrown"));
        Object customObject = new Object() {
            @Override
            public String toString() {
                return "custom toString";
            }
        };
        String result = toString.apply(customObject);
        assertThat(result).isEqualTo("custom toString");
    }

    @Test
    public void testApplyNullThrowsIllegalArgumentException() {
        ToString toString = new ToString(() -> new RuntimeException("should not be thrown"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> toString.apply(null));
    }

    @Test
    public void testApplyToStringThrowsSupplierException() {
        ToString toString = new ToString(() -> new UnsupportedOperationException("toString failed"));
        Object failingObject = new Object() {
            @Override
            public String toString() {
                throw new UnsupportedOperationException("toString failed");
            }
        };
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> toString.apply(failingObject));
    }

    @Test
    public void testConstructorNullSupplier() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ToString(null));
    }
}
