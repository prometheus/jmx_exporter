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

package io.prometheus.jmx.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ConfigurationExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        ConfigurationException exception = new ConfigurationException("test message");
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo("test message");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("original cause");
        ConfigurationException exception = new ConfigurationException("test message", cause);
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo("test message");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    public void testSupplier() {
        java.util.function.Supplier<ConfigurationException> supplier =
                ConfigurationException.supplier("supplier message");
        assertThat(supplier).isNotNull();
        ConfigurationException exception = supplier.get();
        assertThat(exception.getMessage()).isEqualTo("supplier message");
    }

    @Test
    public void testSupplierProducesNewInstanceEachTime() {
        java.util.function.Supplier<ConfigurationException> supplier = ConfigurationException.supplier("new instance");
        ConfigurationException exception1 = supplier.get();
        ConfigurationException exception2 = supplier.get();
        assertThat(exception1).isNotSameAs(exception2);
        assertThat(exception1.getMessage()).isEqualTo(exception2.getMessage());
    }
}
