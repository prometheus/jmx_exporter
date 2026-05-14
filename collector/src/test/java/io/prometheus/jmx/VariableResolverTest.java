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

package io.prometheus.jmx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.prometheus.jmx.variable.VariableResolver;
import io.prometheus.jmx.variable.VariableResolverException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class VariableResolverTest {

    @Nested
    class ResolveVariableTests {

        @Test
        void resolveNullReturnsNull() {
            String result = VariableResolver.resolveVariable(null);
            assertThat(result).isNull();
        }

        @Test
        void resolveEmptyStringReturnsEmptyString() {
            String result = VariableResolver.resolveVariable("");
            assertThat(result).isEqualTo("");
        }

        @Test
        void resolveWhitespaceOnlyReturnsWhitespace() {
            String result = VariableResolver.resolveVariable("   ");
            assertThat(result).isEqualTo("   ");
        }

        @Test
        void resolvePlainStringReturnsPlainString() {
            String result = VariableResolver.resolveVariable("plain_text");
            assertThat(result).isEqualTo("plain_text");
        }

        @Test
        void resolveEmptyBracesReturnsLiteral() {
            String result = VariableResolver.resolveVariable("${}");
            assertThat(result).isEqualTo("${}");
        }

        @Test
        void resolveWhitespaceAroundBracesReturnsLiteral() {
            String result = VariableResolver.resolveVariable("  ${}  ");
            assertThat(result).isEqualTo("  ${}  ");
        }

        @Test
        void resolveExistingEnvironmentVariable() {
            String result = VariableResolver.resolveVariable("${PATH}");
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
        }

        @Test
        void resolveEnvironmentVariableWithWhitespace() {
            String result = VariableResolver.resolveVariable("  ${PATH}  ");
            assertThat(result).isNotNull();
        }

        @Test
        void resolveUndefinedVariableThrowsException() {
            assertThatExceptionOfType(VariableResolverException.class)
                    .isThrownBy(() -> VariableResolver.resolveVariable("${UNDEFINED_VAR_12345}"))
                    .withMessageContaining("UNDEFINED_VAR_12345");
        }

        @Test
        void resolveEmptyBracesWithWhitespaceThrowsException() {
            assertThatExceptionOfType(VariableResolverException.class)
                    .isThrownBy(() -> VariableResolver.resolveVariable("${   }"))
                    .withMessageContaining("Invalid environment variable name");
        }

        @Test
        void resolveVariableNotSurroundedByBracesReturnsLiteral() {
            String result = VariableResolver.resolveVariable("$PATH");
            assertThat(result).isEqualTo("$PATH");
        }

        @Test
        void resolvePartiallyMatchedBracesReturnsLiteral() {
            String result = VariableResolver.resolveVariable("${PATH");
            assertThat(result).isEqualTo("${PATH");
        }

        @Test
        void resolveVariableWithTrailingText() {
            String result = VariableResolver.resolveVariable("${PATH}_suffix");
            assertThat(result).isEqualTo("${PATH}_suffix");
        }

        @Test
        void resolveVariableWithLeadingText() {
            String result = VariableResolver.resolveVariable("prefix_${PATH}");
            assertThat(result).isEqualTo("prefix_${PATH}");
        }
    }

    @Nested
    class EnvironmentVariableExceptionTests {

        @Test
        void undefinedVariableThrowsWithCorrectMessage() {
            String varName = "UNDEFINED_TEST_VAR_ABC123";
            assertThatThrownBy(() -> VariableResolver.resolveVariable("${" + varName + "}"))
                    .isInstanceOf(VariableResolverException.class)
                    .hasMessageContaining(varName)
                    .hasMessageContaining("not defined");
        }

        @Test
        void emptyBracesWithWhitespaceThrowsInvalidNameException() {
            assertThatThrownBy(() -> VariableResolver.resolveVariable("${   }"))
                    .isInstanceOf(VariableResolverException.class)
                    .hasMessageContaining("Invalid environment variable name");
        }
    }

    @Nested
    class EnvironmentVariableValueTests {

        @Test
        void resolveExistingVariableWithNonEmptyValue() {
            String pathValue = System.getenv("PATH");
            if (pathValue != null && !pathValue.trim().isEmpty()) {
                String result = VariableResolver.resolveVariable("${PATH}");
                assertThat(result).isNotNull();
                assertThat(result).isNotEmpty();
            }
        }
    }
}
