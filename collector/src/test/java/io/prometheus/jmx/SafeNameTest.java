package io.prometheus.jmx;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SafeNameTest {
    @Parameterized.Parameters(name = "{index}: testSafeName(expected={0} actual={1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "test_test", "test-test" }, { "test_test", "test-_test" }, { "test_test", "test-_-test" },
                { "_", "-_-"}, { "", "" }, { null, null }, { "_", "---" },
                { "test", "test" },
                // A very long string
                { "_asetstjlk_testkljsek_tesktjsekrslk_testkljsetkl_tkesjtk_sljtslkjetesslelse_lktsjetlkesltel_kesjltelksjetkl_tesktjksjltse_sljteslselkselse_tsjetlksetklsjekl_slkfjrtlskek_",
                "$asetstjlk_$testkljsek_$tesktjsekrslk_$testkljsetkl_$tkesjtk_$sljtslkjetesslelse_$lktsjetlkesltel_$kesjltelksjetkl_$tesktjksjltse_$sljteslselkselse_$tsjetlksetklsjekl_$slkfjrtlskek___" },
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
