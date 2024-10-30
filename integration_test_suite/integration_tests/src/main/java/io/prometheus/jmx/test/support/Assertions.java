package io.prometheus.jmx.test.support;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.http.HttpResponseBody;

/** Class to implement Assertions */
public class Assertions {

    /** Constructor */
    private Assertions() {
        // INTENTIONALLY BLANK
    }

    /**
     * Assert a health response
     *
     * @param httpResponse httpResponse
     */
    public static void assertHealthyResponse(HttpResponse httpResponse) {
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().string()).isNotBlank();
        assertThat(httpResponse.body().string()).contains("Exporter is healthy.");
    }

    /**
     * Assert common metrics response
     *
     * @param httpResponse httpResponse
     */
    public static void assertCommonMetricsResponse(HttpResponse httpResponse) {
        assertThat(httpResponse).isNotNull();

        int statusCode = httpResponse.statusCode();
        if (statusCode != 200) {
            HttpResponseBody body = httpResponse.body();
            if (body != null) {
                throw new AssertionError(
                        format(
                                "Expected statusCode [%d] but was [%d] body [%s]",
                                200, statusCode, body.string()));
            } else {
                throw new AssertionError(
                        format("Expected statusCode [%d] but was [%d] no body", 200, statusCode));
            }
        }

        assertThat(httpResponse.headers()).isNotNull();
        assertThat(httpResponse.headers().get("CONTENT-TYPE")).hasSize(1);
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().bytes()).isNotNull();
        assertThat(httpResponse.body().bytes().length).isGreaterThan(0);
    }
}
