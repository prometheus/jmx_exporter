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

import java.util.regex.Pattern;
import java.util.stream.Stream;
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
}
