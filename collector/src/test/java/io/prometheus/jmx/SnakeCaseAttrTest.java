/*
 * Copyright (C) 2018-2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SnakeCaseAttrTest {
    @Parameterized.Parameters(name = "{index}: testAttrToSnakeAndLowerCase(expected={0} actual={1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                    {"test_test", "testTest"},
                    {"test_test_test", "testTestTest"},
                    {"test_test", "test_test"},
                    {"test1", "test1"},
                    {"start_time_$1_$2", "StartTime_$1_$2"},
                    {"a", "A"},
                    {"aa", "AA"},
                    {"tcp", "TCP"},
                    {"test_tcptest", "testTCPTest"},
                    {null, null},
                    {"", ""},
                    {" ", " "},
                    {"test_test\n_test", "testTest\nTest"},
                    {"test_test", "test_Test"},
                    {"_test_test", "_Test_Test"}
                });
    }

    private final String expected;
    private final String input;

    public SnakeCaseAttrTest(String expected, String input) {
        this.expected = expected;
        this.input = input;
    }

    @Test
    public void testAttrToSnakeAndLowerCase() {
        String snakeAndLowerString = JmxCollector.toSnakeAndLowerCase(input);
        assertEquals(expected, snakeAndLowerString);
    }
}
