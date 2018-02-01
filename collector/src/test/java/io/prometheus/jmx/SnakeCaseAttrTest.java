package io.prometheus.jmx;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SnakeCaseAttrTest {
    @Parameterized.Parameters(name = "{index}: testAttrToSnakeAndLowerCase(expected={0} actual={1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "test_test", "testTest" }, { "test_test_test", "testTestTest" }, {"test_test", "test_test"},
                { "test1", "test1"}, { "start_time_$1_$2", "StartTime_$1_$2" }, { "a", "A" }, { "aa", "AA" },
                { "tcp", "TCP" }, { "test_tcptest", "testTCPTest" }, { null, null },  { "", "" }, { " ", " " },
                { "test_test\n_test", "testTest\nTest" }, { "test_test", "test_Test" }, { "_test_test", "_Test_Test"}
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
