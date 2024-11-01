/*
 * Copyright (C) 2018-present The Prometheus jmx_exporter Authors
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
public class SafeNameTest {
    @Parameterized.Parameters(name = "{index}: testSafeName(expected={0} actual={1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                    {"test_test", "test-test"},
                    {"test_test", "test-_test"},
                    {"test_test", "test-_-test"},
                    {"_", "-_-"},
                    {"", ""},
                    {null, null},
                    {"_", "---"},
                    {"test", "test"},
                    {"_001", "001"},
                    // A very long string
                    {
                        "_asetstjlk_testkljsek_tesktjsekrslk_testkljsetkl_tkesjtk_sljtslkjetesslelse_lktsjetlkesltel_kesjltelksjetkl_tesktjksjltse_sljteslselkselse_tsjetlksetklsjekl_slkfjrtlskek_",
                        "$asetstjlk_$testkljsek_$tesktjsekrslk_$testkljsetkl_$tkesjtk_$sljtslkjetesslelse_$lktsjetlkesltel_$kesjltelksjetkl_$tesktjksjltse_$sljteslselkselse_$tsjetlksetklsjekl_$slkfjrtlskek___"
                    },
                    {"test_swedish_chars_", "test_swedish_chars_åäö"},
                    {"test_test", "test@test"},
                    {"test_test", "test;test"},
                    {"test:test", "test:test"},
                });
    }

    private final String expected;
    private final String input;

    public SafeNameTest(String expected, String input) {
        this.expected = expected;
        this.input = input;
    }

    @Test
    public void testSafeName() {
        String safeName = JmxCollector.safeName(input);
        assertEquals(expected, safeName);
    }
}
