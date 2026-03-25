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

public class IsURLTest {

    @Test
    public void testApplyValidHttpUrl() {
        IsURL isURL = new IsURL(() -> new RuntimeException("should not be thrown"));
        assertThat(isURL.apply("http://localhost:8080")).isEqualTo("http://localhost:8080");
    }

    @Test
    public void testApplyValidHttpsUrl() {
        IsURL isURL = new IsURL(() -> new RuntimeException("should not be thrown"));
        assertThat(isURL.apply("https://example.com")).isEqualTo("https://example.com");
    }

    @Test
    public void testApplyValidUrlWithPath() {
        IsURL isURL = new IsURL(() -> new RuntimeException("should not be thrown"));
        assertThat(isURL.apply("http://localhost:8080/metrics")).isEqualTo("http://localhost:8080/metrics");
    }

    @Test
    public void testApplyValidUrlWithQueryParams() {
        IsURL isURL = new IsURL(() -> new RuntimeException("should not be thrown"));
        assertThat(isURL.apply("http://localhost:8080/path?key=value&key2=value2"))
                .isEqualTo("http://localhost:8080/path?key=value&key2=value2");
    }

    @Test
    public void testApplyValidUrlWithFragment() {
        IsURL isURL = new IsURL(() -> new RuntimeException("should not be thrown"));
        assertThat(isURL.apply("http://localhost:8080/path#fragment")).isEqualTo("http://localhost:8080/path#fragment");
    }

    @Test
    public void testApplyEmptyStringThrowsSupplierException() {
        IsURL isURL = new IsURL(() -> new UnsupportedOperationException("invalid URL"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> isURL.apply(""));
    }

    @Test
    public void testApplyBlankStringThrowsSupplierException() {
        IsURL isURL = new IsURL(() -> new UnsupportedOperationException("invalid URL"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> isURL.apply("   "));
    }

    @Test
    public void testApplyMalformedUrlNoSchemeThrowsSupplierException() {
        IsURL isURL = new IsURL(() -> new UnsupportedOperationException("invalid URL"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> isURL.apply("localhost:8080"));
    }

    @Test
    public void testApplyMalformedUrlInvalidCharsThrowsIllegalArgumentException() {
        IsURL isURL = new IsURL(() -> new UnsupportedOperationException("invalid URL"));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> isURL.apply("http://[invalid url]/path"));
    }

    @Test
    public void testConstructorNullSupplier() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new IsURL(null));
    }
}
