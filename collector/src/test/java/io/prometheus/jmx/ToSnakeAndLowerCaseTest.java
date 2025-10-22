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

import java.util.stream.Stream;
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
}
