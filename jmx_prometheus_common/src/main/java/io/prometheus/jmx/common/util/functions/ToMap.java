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

import io.prometheus.jmx.common.util.Precondition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** Function to convert an Object to a Map */
@SuppressWarnings("unchecked")
public class ToMap implements Function<Object, Map<String, String>> {

    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructor
     *
     * @param supplier supplier
     */
    public ToMap(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    @Override
    public Map<String, String> apply(Object o) {
        try {
            Map<String, String> result = new LinkedHashMap<>();
            Map<Object, Object> map = (Map<Object, Object>) o;

            map.forEach((o1, o2) -> result.put(o1.toString().trim(), o2.toString().trim()));

            return result;
        } catch (Throwable t) {
            throw supplier.get();
        }
    }
}
