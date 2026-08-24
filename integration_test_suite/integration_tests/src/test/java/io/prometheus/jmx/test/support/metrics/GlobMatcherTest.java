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

package io.prometheus.jmx.test.support.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GlobMatcherTest {

    @Test
    void singleWildcardMatchesEverything() {
        assertThat(MetricsAssertions.matchesGlob("*", "")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*", "anything")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*", "with spaces")).isTrue();
    }

    @Test
    void exactMatch() {
        assertThat(MetricsAssertions.matchesGlob("hello", "hello")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("hello", "hell")).isFalse();
        assertThat(MetricsAssertions.matchesGlob("hello", "helloo")).isFalse();
        assertThat(MetricsAssertions.matchesGlob("hello", "Hello")).isFalse();
    }

    @Test
    void prefixWildcard() {
        assertThat(MetricsAssertions.matchesGlob("pre*", "prefix")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("pre*", "pre")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("pre*", "pr")).isFalse();
        assertThat(MetricsAssertions.matchesGlob("pre*", "other")).isFalse();
    }

    @Test
    void suffixWildcard() {
        assertThat(MetricsAssertions.matchesGlob("*suf", "endsuf")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*suf", "suf")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*suf", "suffix")).isFalse();
        assertThat(MetricsAssertions.matchesGlob("*suf", "other")).isFalse();
    }

    @Test
    void containsWildcard() {
        assertThat(MetricsAssertions.matchesGlob("*mid*", "has middle here")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*mid*", "middle")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*mid*", "mid")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*mid*", "no match")).isFalse();
    }

    @Test
    void prefixAndSuffix() {
        assertThat(MetricsAssertions.matchesGlob("a*z", "az")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("a*z", "abz")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("a*z", "axyz")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("a*z", "a")).isFalse();
        assertThat(MetricsAssertions.matchesGlob("a*z", "z")).isFalse();
        assertThat(MetricsAssertions.matchesGlob("a*z", "azb")).isFalse();
    }

    @Test
    void multipleWildcards() {
        assertThat(MetricsAssertions.matchesGlob("*foo*bar", "xfooybar")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*foo*bar", "foobar")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*foo*bar", "fooxxx")).isFalse();
        assertThat(MetricsAssertions.matchesGlob("*foo*bar", "barfoo")).isFalse();

        assertThat(MetricsAssertions.matchesGlob("foo*bar*", "foobarx")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("foo*bar*", "foobar")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("foo*bar*", "fooxbar")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("foo*bar*", "barfoo")).isFalse();

        assertThat(MetricsAssertions.matchesGlob("*foo*bar*", "xfooybarz")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*foo*bar*", "foobar")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*foo*bar*", "fooxbar")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*foo*bar*", "barfoobaz")).isFalse();
    }

    @Test
    void threeWildcards() {
        assertThat(MetricsAssertions.matchesGlob("a*b*c*d", "aXbYcZd")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("a*b*c*d", "abcd")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("a*b*c*d", "aXbYc")).isFalse();
        assertThat(MetricsAssertions.matchesGlob("a*b*c*d", "bYcZd")).isFalse();
    }

    @Test
    void allWildcards() {
        assertThat(MetricsAssertions.matchesGlob("*a*b*c*", "XaYbZcW")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*a*b*c*", "abc")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*a*b*c*", "aXbYc")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*a*b*c*", "abcW")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("*a*b*c*", "XaYcZbW")).isFalse();
        assertThat(MetricsAssertions.matchesGlob("*a*b*c*", "xxx")).isFalse();
        assertThat(MetricsAssertions.matchesGlob("*a*b*c*", "cba")).isFalse();
    }

    @Test
    void emptySegments() {
        assertThat(MetricsAssertions.matchesGlob("**", "anything")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("a**b", "axb")).isTrue();
        assertThat(MetricsAssertions.matchesGlob("a**b", "ab")).isTrue();
    }

    @ParameterizedTest
    @MethodSource("jvmVersionExamples")
    void jvmVersionGlobPatterns(String pattern, String value, boolean expected) {
        assertThat(MetricsAssertions.matchesGlob(pattern, value)).isEqualTo(expected);
    }

    static Stream<Arguments> jvmVersionGlobPatterns() {
        return Stream.of(
                // Exact version (no wildcards)
                Arguments.of("1.8.0_492-b09", "1.8.0_492-b09", true),
                Arguments.of("1.8.0_492-b09", "1.8.0_502-b09", false),

                // Any version
                Arguments.of("*", "25.0.3+9-LTS", true),

                // Major version prefix
                Arguments.of("1.8*", "1.8.0_492-b09", true),
                Arguments.of("1.8*", "11.0.31+11-LTS", false),

                // LTS suffix
                Arguments.of("*-LTS", "25.0.3+9-LTS", true),
                Arguments.of("*-LTS", "25.0.3+9", false),

                // Contains jvmci
                Arguments.of("*jvmci*", "17.0.4+8-jvmci-22.3-b22", true),
                Arguments.of("*jvmci*", "17.0.4+8", false),

                // Starts with major, ends with build
                Arguments.of("17*b09", "17.0.19+10-b09", true),
                Arguments.of("17*b09", "17.0.19+10-b10", false),

                // Multiple wildcards
                Arguments.of("*0*LTS", "21.0.11+10-LTS", true),
                Arguments.of("*0*LTS", "21.0.11+10", false));
    }
}
