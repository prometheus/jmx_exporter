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

import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ToSnakeAndLowerCaseTest {

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of("testTest", "test_test"),
                Arguments.of("testTestTest", "test_test_test"),
                Arguments.of("test_test", "test_test"),
                Arguments.of("test1", "test1"),
                Arguments.of("StartTime_$1_$2", "start_time_$1_$2"),
                Arguments.of("A", "a"),
                Arguments.of("AA", "aa"),
                Arguments.of("TCP", "tcp"),
                Arguments.of("testTCPTest", "test_tcptest"),
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of(" ", " "),
                Arguments.of("testTest\nTest", "test_test\n_test"),
                Arguments.of("test_Test", "test_test"),
                Arguments.of("_Test_Test", "_test_test"));
    }

    @ParameterizedTest(name = "{index} => input={0}, expected={1}")
    @MethodSource("arguments")
    public void testToSnakeAndLowerCase(String input, String expected) {
        String actual = JmxCollector.toSnakeAndLowerCase(input);

        assertThat(actual).isEqualTo(expected);
    }

    /**
     * Pins the fast path to the general implementation for edge cases and randomized inputs,
     * including title-case characters whose lowercase mapping differs from themselves.
     */
    @Test
    public void testToSnakeAndLowerCaseMatchesReferenceImplementation() {
        Random random = new Random(20240917L);
        char[] alphabet = ("abzAZ09_ :." + '\u01c5' + '\u00e5' + '\u00c5').toCharArray();

        for (int i = 0; i < 100_000; i++) {
            String input = randomString(random, alphabet);
            assertThat(JmxCollector.toSnakeAndLowerCase(input))
                    .as("input=[%s]", input)
                    .isEqualTo(referenceToSnakeAndLowerCase(input));
        }

        String[] edges = {
            null,
            "",
            " ",
            "a",
            "A",
            "AA",
            "aA",
            "Aa",
            "a_A",
            "_A_",
            "\u01c5",
            "a\u01c5b",
            "testTest",
            "test_test",
            "testTCPTest",
            "StartTime_$1_$2"
        };
        for (String edge : edges) {
            assertThat(JmxCollector.toSnakeAndLowerCase(edge))
                    .as("input=[%s]", edge)
                    .isEqualTo(referenceToSnakeAndLowerCase(edge));
        }
    }

    private static String randomString(Random random, char[] alphabet) {
        int length = random.nextInt(24);
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet[random.nextInt(alphabet.length)]);
        }
        return builder.toString();
    }

    // Reference implementation: the original toSnakeAndLowerCase without the fast path.
    private static String referenceToSnakeAndLowerCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        char firstChar = name.charAt(0);
        boolean prevCharIsUpperCaseOrUnderscore = Character.isUpperCase(firstChar) || firstChar == '_';
        StringBuilder stringBuilder = new StringBuilder(name.length()).append(Character.toLowerCase(firstChar));

        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean charIsUpperCase = Character.isUpperCase(c);
            if (!prevCharIsUpperCaseOrUnderscore && charIsUpperCase) {
                stringBuilder.append("_");
            }
            stringBuilder.append(Character.toLowerCase(c));
            prevCharIsUpperCaseOrUnderscore = charIsUpperCase || c == '_';
        }

        return stringBuilder.toString();
    }
}
