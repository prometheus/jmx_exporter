package io.prometheus.jmx;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.fail;

public class FilteringSafeConstructorTest {

    @Test
    public void testSafeYaml() throws IOException {
        new Yaml(new FilteringSafeConstructor()).load(toString("/safe.yaml"));
    }

    @Test
    public void testUnsafeYaml() {
        try {
            new Yaml(new FilteringSafeConstructor()).load(toString("/unsafe.yaml"));
            fail("Expected ConstructorException");
        } catch (Throwable t) {
            if (!(t instanceof ConstructorException)) {
                fail("Expected ConstructorException");
            }
        }
    }

    @Test
    public void testSecurityThreatYaml() {
        try {
            new Yaml(new FilteringSafeConstructor()).load(toString("/security_threat.yaml"));
            fail("Expected ConstructorException");
        } catch (Throwable t) {
            if (!(t instanceof ConstructorException)) {
                fail("Expected ConstructorException");
            }
        }
    }

    private static String toString(String resource) throws IOException {
        InputStream inputStream = null;

        try {
            inputStream = FilteringSafeConstructorTest.class.getResourceAsStream(resource);
            return toString(inputStream);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable t) {
                    // DO NOTHING
                }
            }
        }
    }

    private static String toString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        while (true) {
            int i = inputStream.read();

            if (i == -1) {
                break;
            }

            stringBuilder.append((char) i);
        }

        return stringBuilder.toString();
    }
}
