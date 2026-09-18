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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ToSafeNameTest {

    private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_:]*");

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of("test-test", "test_test"),
                Arguments.of("test-_test", "test_test"),
                Arguments.of("test-_-test", "test_test"),
                Arguments.of("-_-", "_"),
                Arguments.of("", ""),
                Arguments.of(null, null),
                Arguments.of("---", "_"),
                Arguments.of("__test", "_test"),
                Arguments.of("____test", "_test"),
                Arguments.of("test", "test"),
                Arguments.of("001", "_001"),
                // Arguments.of("__001", "__001"),
                Arguments.of(
                        "$asetstjlk_$testkljsek_$tesktjsekrslk_$testkljsetkl_$tkesjtk_$sljtslkjetesslelse_$lktsjetlkesltel_$kesjltelksjetkl_$tesktjksjltse_$sljteslselkselse_$tsjetlksetklsjekl_$slkfjrtlskek___",
                        "_asetstjlk_testkljsek_tesktjsekrslk_testkljsetkl_tkesjtk_sljtslkjetesslelse_lktsjetlkesltel_kesjltelksjetkl_tesktjksjltse_sljteslselkselse_tsjetlksetklsjekl_slkfjrtlskek_"),
                Arguments.of("test_swedish_chars_åäö", "test_swedish_chars_"),
                Arguments.of("test@test", "test_test"),
                Arguments.of("test;test", "test_test"),
                Arguments.of("test:test", "test:test"));
    }

    @ParameterizedTest(name = "{index} => input={0}, expected={1}")
    @MethodSource("arguments")
    public void testToSafeName(String input, String expected) {
        String actual = JmxCollector.toSafeName(input);

        assertThat(actual).isEqualTo(expected);

        if (input != null && !input.isEmpty()) {
            assertThat(VALID_NAME.matcher(actual)).matches();
        }
    }

    /**
     * Pins the fast path to the general implementation for edge cases and randomized inputs.
     */
    @Test
    public void testToSafeNameMatchesReferenceImplementation() {
        Random random = new Random(20240917L);
        char[] alphabet = ("abzAZ09_:-.@$" + '\u00e5' + '\u00f6' + '\u4e2d').toCharArray();

        for (int i = 0; i < 100_000; i++) {
            String input = randomString(random, alphabet);
            assertThat(JmxCollector.toSafeName(input)).as("input=[%s]", input).isEqualTo(referenceToSafeName(input));
        }

        String[] edges = {
            null,
            "",
            "a",
            "_",
            "__",
            "___",
            ":",
            "::",
            "a:",
            ":a",
            "0",
            "00",
            "0a",
            "a0",
            "__001",
            "_0",
            "1abc",
            "abc_",
            "abc__def",
            "a-b",
            "a_b",
            "A_B",
            "test:test",
            "\u4e2d\u6587"
        };
        for (String edge : edges) {
            assertThat(JmxCollector.toSafeName(edge)).as("input=[%s]", edge).isEqualTo(referenceToSafeName(edge));
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

    // Reference implementation: the original toSafeName without the fast path.
    private static String referenceToSafeName(String name) {
        if (name == null) {
            return null;
        }

        boolean prevCharIsUnderscore = false;
        StringBuilder stringBuilder = new StringBuilder(name.length());

        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            stringBuilder.append("_");
        }

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean isUnsafeChar = !isLegalCharacter(c);
            if ((isUnsafeChar || c == '_')) {
                if (!prevCharIsUnderscore) {
                    stringBuilder.append("_");
                    prevCharIsUnderscore = true;
                }
            } else {
                stringBuilder.append(c);
                prevCharIsUnderscore = false;
            }
        }

        return stringBuilder.toString();
    }

    private static boolean isLegalCharacter(char input) {
        return ((input == ':')
                || (input == '_')
                || (input >= 'a' && input <= 'z')
                || (input >= 'A' && input <= 'Z')
                || (input >= '0' && input <= '9'));
    }
}
