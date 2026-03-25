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

import io.prometheus.jmx.common.util.MapAccessor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ToMapAccessorTest {

    @Test
    public void testApplyValidMap() {
        ToMapAccessor toMapAccessor = new ToMapAccessor(() -> new RuntimeException("should not be thrown"));
        Map<Object, Object> input = new LinkedHashMap<>();
        input.put("key", "value");
        MapAccessor result = toMapAccessor.apply(input);
        assertThat(result).isNotNull();
        assertThat(result.containsPath("/key")).isTrue();
    }

    @Test
    public void testApplyNullThrowsIllegalArgumentException() {
        ToMapAccessor toMapAccessor =
                new ToMapAccessor(() -> new UnsupportedOperationException("map accessor conversion failed"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> toMapAccessor.apply(null));
    }

    @Test
    public void testApplyNonMapThrowsSupplierException() {
        ToMapAccessor toMapAccessor =
                new ToMapAccessor(() -> new UnsupportedOperationException("map accessor conversion failed"));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> toMapAccessor.apply("not a map"));
    }

    @Test
    public void testConstructorNullSupplier() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ToMapAccessor(null));
    }
}
