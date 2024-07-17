package io.prometheus.jmx.test.http.ssl;

import io.prometheus.jmx.test.common.AbstractExporterTest;
import io.prometheus.jmx.test.common.ExporterTestEnvironment;
import io.prometheus.jmx.test.http.authentication.AbstractBasicAuthenticationTest;
import java.util.stream.Stream;
import org.antublue.verifyica.api.Verifyica;

public abstract class AbstractSSLTest extends AbstractExporterTest {

    private static final String BASE_URL = "https://localhost";

    /**
     * Method to get the Stream of test environments
     *
     * @return the Stream of test environments
     */
    @Verifyica.ArgumentSupplier
    public static Stream<ExporterTestEnvironment> arguments() {
        return AbstractBasicAuthenticationTest.arguments()
                .map(exporterTestEnvironment -> exporterTestEnvironment.setBaseUrl(BASE_URL));
    }
}
