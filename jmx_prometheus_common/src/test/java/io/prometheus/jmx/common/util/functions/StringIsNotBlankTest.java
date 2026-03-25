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

public class StringIsNotBlankTest {

    @Test
    public void testApplyValidString() {
        StringIsNotBlank stringIsNotBlank = new StringIsNotBlank(() -> new RuntimeException("should not be thrown"));
        assertThat(stringIsNotBlank.apply("hello")).isEqualTo("hello");
    }

    @Test
    public void testApplyStringWithWhitespace() {
        StringIsNotBlank stringIsNotBlank = new StringIsNotBlank(() -> new RuntimeException("should not be thrown"));
        assertThat(stringIsNotBlank.apply("  hello world  ")).isEqualTo("  hello world  ");
    }

    @Test
    public void testApplyEmptyStringThrowsSupplierException() {
        StringIsNotBlank stringIsNotBlank =
                new StringIsNotBlank(() -> new UnsupportedOperationException("blank string"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> stringIsNotBlank.apply(""));
    }

    @Test
    public void testApplyWhitespaceOnlyThrowsSupplierException() {
        StringIsNotBlank stringIsNotBlank =
                new StringIsNotBlank(() -> new UnsupportedOperationException("blank string"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> stringIsNotBlank.apply("   "));
    }

    @Test
    public void testApplyTabOnlyThrowsSupplierException() {
        StringIsNotBlank stringIsNotBlank =
                new StringIsNotBlank(() -> new UnsupportedOperationException("blank string"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> stringIsNotBlank.apply("\t"));
    }

    @Test
    public void testApplyNewlineOnlyThrowsSupplierException() {
        StringIsNotBlank stringIsNotBlank =
                new StringIsNotBlank(() -> new UnsupportedOperationException("blank string"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> stringIsNotBlank.apply("\n"));
    }

    @Test
    public void testApplyMixedWhitespaceThrowsSupplierException() {
        StringIsNotBlank stringIsNotBlank =
                new StringIsNotBlank(() -> new UnsupportedOperationException("blank string"));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> stringIsNotBlank.apply(" \t \r \n "));
    }

    @Test
    public void testConstructorNullSupplier() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new StringIsNotBlank(null));
    }
}
