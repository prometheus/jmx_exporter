/*
 * Copyright (C) 2025-present The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.common.configuration;

import io.prometheus.jmx.common.util.Precondition;
import java.util.function.Function;
import java.util.function.Supplier;

/** Class to implement ConvertToBoolean */
public class ConvertToBoolean implements Function<Object, Boolean> {

    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructor
     *
     * @param supplier supplier
     */
    public ConvertToBoolean(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    /**
     * Method to apply a function
     *
     * @param value value
     * @return the return value
     */
    @Override
    public Boolean apply(Object value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        try {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                return Boolean.valueOf(value.toString());
            }
        } catch (Throwable t) {
            throw supplier.get();
        }
    }
}
