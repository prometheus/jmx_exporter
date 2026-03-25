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

public class ToMapTest {

    @Test
    public void testApplyValidMap() {
        ToMap toMap = new ToMap(() -> new RuntimeException("should not be thrown"));
        Map<Object, Object> input = new LinkedHashMap<>();
        input.put("key1", "value1");
        input.put("key2", "value2");
        Map<String, String> result = toMap.apply(input);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key2")).isEqualTo("value2");
    }

    @Test
    public void testApplyTrimsKeysAndValues() {
        ToMap toMap = new ToMap(() -> new RuntimeException("should not be thrown"));
        Map<Object, Object> input = new LinkedHashMap<>();
        input.put(" key1 ", " value1 ");
        Map<String, String> result = toMap.apply(input);
        assertThat(result.get("key1")).isEqualTo("value1");
    }

    @Test
    public void testApplyEmptyMap() {
        ToMap toMap = new ToMap(() -> new RuntimeException("should not be thrown"));
        Map<Object, Object> input = new LinkedHashMap<>();
        Map<String, String> result = toMap.apply(input);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void testApplyNullThrowsSupplierException() {
        ToMap toMap = new ToMap(() -> new UnsupportedOperationException("map conversion failed"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> toMap.apply(null));
    }

    @Test
    public void testApplyNonMapThrowsSupplierException() {
        ToMap toMap = new ToMap(() -> new UnsupportedOperationException("map conversion failed"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> toMap.apply("not a map"));
    }

    @Test
    public void testConstructorNullSupplier() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ToMap(null));
    }
}
