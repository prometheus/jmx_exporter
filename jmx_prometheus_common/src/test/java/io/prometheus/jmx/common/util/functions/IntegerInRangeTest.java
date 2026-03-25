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

public class IntegerInRangeTest {

    @Test
    public void testApplyValueAtMinimum() {
        IntegerInRange integerInRange = new IntegerInRange(1, 10, () -> new RuntimeException("should not be thrown"));
        assertThat(integerInRange.apply(1)).isEqualTo(1);
    }

    @Test
    public void testApplyValueAtMaximum() {
        IntegerInRange integerInRange = new IntegerInRange(1, 10, () -> new RuntimeException("should not be thrown"));
        assertThat(integerInRange.apply(10)).isEqualTo(10);
    }

    @Test
    public void testApplyValueWithinRange() {
        IntegerInRange integerInRange = new IntegerInRange(1, 10, () -> new RuntimeException("should not be thrown"));
        assertThat(integerInRange.apply(5)).isEqualTo(5);
    }

    @Test
    public void testApplyValueBelowMinimumThrowsSupplierException() {
        IntegerInRange integerInRange =
                new IntegerInRange(1, 10, () -> new UnsupportedOperationException("out of range"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> integerInRange.apply(0));
    }

    @Test
    public void testApplyValueAboveMaximumThrowsSupplierException() {
        IntegerInRange integerInRange =
                new IntegerInRange(1, 10, () -> new UnsupportedOperationException("out of range"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> integerInRange.apply(11));
    }

    @Test
    public void testApplyNegativeRange() {
        IntegerInRange integerInRange =
                new IntegerInRange(-100, -50, () -> new RuntimeException("should not be thrown"));
        assertThat(integerInRange.apply(-75)).isEqualTo(-75);
    }

    @Test
    public void testApplyNegativeRangeBelowMinimumThrows() {
        IntegerInRange integerInRange =
                new IntegerInRange(-100, -50, () -> new UnsupportedOperationException("out of range"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> integerInRange.apply(-101));
    }

    @Test
    public void testConstructorNullSupplier() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new IntegerInRange(1, 10, null));
    }
}
