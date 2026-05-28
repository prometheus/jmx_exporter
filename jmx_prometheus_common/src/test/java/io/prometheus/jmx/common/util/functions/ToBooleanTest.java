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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ToBooleanTest {

    @Test
    public void testApplyBooleanInput() {
        ToBoolean toBoolean = ToBoolean.of(() -> new RuntimeException("should not be thrown"));
        assertThat(toBoolean.apply(Boolean.TRUE)).isTrue();
        assertThat(toBoolean.apply(Boolean.FALSE)).isFalse();
    }

    @Test
    public void testApplyTrueString() {
        ToBoolean toBoolean = ToBoolean.of(() -> new RuntimeException("should not be thrown"));
        assertThat(toBoolean.apply("true")).isTrue();
        assertThat(toBoolean.apply("TRUE")).isTrue();
        assertThat(toBoolean.apply("True")).isTrue();
    }

    @Test
    public void testApplyFalseString() {
        ToBoolean toBoolean = ToBoolean.of(() -> new RuntimeException("should not be thrown"));
        assertThat(toBoolean.apply("false")).isFalse();
        assertThat(toBoolean.apply("FALSE")).isFalse();
        assertThat(toBoolean.apply("False")).isFalse();
    }

    @Test
    public void testApplyNullThrowsIllegalArgumentException() {
        ToBoolean toBoolean = ToBoolean.of(() -> new RuntimeException("should not be thrown"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> toBoolean.apply(null));
    }

    @Test
    public void testApplyInvalidStringThrows() {
        ToBoolean toBoolean = ToBoolean.of(() -> new RuntimeException("invalid boolean"));
        assertThatThrownBy(() -> toBoolean.apply("not a boolean")).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testApplyEmptyStringThrows() {
        ToBoolean toBoolean = ToBoolean.of(() -> new RuntimeException("invalid boolean"));
        assertThatThrownBy(() -> toBoolean.apply("")).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testConstructorNullSupplier() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ToBoolean.of(null));
    }

    @Test
    public void testApplyThrowsSupplierExceptionOnCastException() {
        RuntimeException expectedException = new RuntimeException("conversion failed");
        ToBoolean toBoolean = ToBoolean.of(() -> expectedException);

        Object nonStringNonBoolean = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("toString explosion");
            }
        };

        assertThatThrownBy(() -> toBoolean.apply(nonStringNonBoolean)).isSameAs(expectedException);
    }
}
