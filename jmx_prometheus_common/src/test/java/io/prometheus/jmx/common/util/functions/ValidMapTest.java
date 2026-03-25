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

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ValidMapTest {

    @Test
    public void testApplyValidMap() {
        ValidMap validMap = new ValidMap(() -> new RuntimeException("should not be thrown"));
        Map<String, String> input = new LinkedHashMap<>();
        input.put("key1", "value1");
        input.put("key2", "value2");
        Map<String, String> result = validMap.apply(input);
        assertThat(result).isSameAs(input);
    }

    @Test
    public void testApplyEmptyMap() {
        ValidMap validMap = new ValidMap(() -> new RuntimeException("should not be thrown"));
        Map<String, String> input = new LinkedHashMap<>();
        Map<String, String> result = validMap.apply(input);
        assertThat(result).isSameAs(input);
    }

    @Test
    public void testApplyNullKeyThrowsSupplierException() {
        ValidMap validMap = new ValidMap(() -> new UnsupportedOperationException("invalid map"));
        Map<String, String> input = new LinkedHashMap<>();
        input.put(null, "value");
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> validMap.apply(input));
    }

    @Test
    public void testApplyEmptyKeyThrowsSupplierException() {
        ValidMap validMap = new ValidMap(() -> new UnsupportedOperationException("invalid map"));
        Map<String, String> input = new LinkedHashMap<>();
        input.put("", "value");
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> validMap.apply(input));
    }

    @Test
    public void testApplyBlankKeyThrowsSupplierException() {
        ValidMap validMap = new ValidMap(() -> new UnsupportedOperationException("invalid map"));
        Map<String, String> input = new LinkedHashMap<>();
        input.put("   ", "value");
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> validMap.apply(input));
    }

    @Test
    public void testApplyNullValueThrowsSupplierException() {
        ValidMap validMap = new ValidMap(() -> new UnsupportedOperationException("invalid map"));
        Map<String, String> input = new LinkedHashMap<>();
        input.put("key", null);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> validMap.apply(input));
    }

    @Test
    public void testApplyEmptyValueThrowsSupplierException() {
        ValidMap validMap = new ValidMap(() -> new UnsupportedOperationException("invalid map"));
        Map<String, String> input = new LinkedHashMap<>();
        input.put("key", "");
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> validMap.apply(input));
    }

    @Test
    public void testConstructorNullSupplier() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ValidMap(null));
    }
}
