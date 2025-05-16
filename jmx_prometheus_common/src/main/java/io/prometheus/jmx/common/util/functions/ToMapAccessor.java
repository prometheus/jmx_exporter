/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.common.util.functions;

import io.prometheus.jmx.common.util.MapAccessor;
import io.prometheus.jmx.common.util.Precondition;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** Function to create a MapAccessor from an Object */
@SuppressWarnings("unchecked")
public class ToMapAccessor implements Function<Object, MapAccessor> {

    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructor
     *
     * @param supplier supplier
     */
    public ToMapAccessor(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    @Override
    public MapAccessor apply(Object value) {
        try {
            return MapAccessor.of((Map<Object, Object>) value);
        } catch (ClassCastException e) {
            throw supplier.get();
        }
    }
}
