package io.prometheus.jmx.test.support.http;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpResponseAssertions {

    private HttpResponseAssertions() {
        // DO NOTHING
    }

    public static void assertHttpResponseCode(HttpResponse httpResponse, int code) {
        assertThat(httpResponse.statusCode()).isEqualTo(code);
    }

    public static void assertHttpResponseHasHeaders(HttpResponse httpResponse) {
        assertThat(httpResponse.headers()).isNotNull();
    }

    public static void assertHttpResponseHasHeader(HttpResponse httpResponse, String name) {
        assertThat(httpResponse.headers()).isNotNull();
        assertThat(httpResponse.headers().get(name)).isNotNull();
    }

    public static void assertHttpResponseHasBody(HttpResponse httpResponse) {
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().bytes().length).isGreaterThan(0);
    }

    public static void assertHttpHealthyResponse(HttpResponse httpResponse) {
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().string().length()).isGreaterThan(0);
        assertThat(httpResponse.body().string()).isEqualTo("Exporter is healthy.\n");
    }

    public static void assertHttpMetricsResponse(HttpResponse httpResponse) {
        assertThat(httpResponse).isNotNull();

        int statusCode = httpResponse.statusCode();
        if (statusCode != 200) {
            HttpResponseBody httpResponseBody = httpResponse.body();
            if (httpResponseBody != null) {
                String content = httpResponseBody.string();
                throw new AssertionError(
                        format(
                                "Expected statusCode [%d] but was [%d] content [%s]",
                                200, statusCode, content));
            } else {
                throw new AssertionError(
                        format(
                                "Expected statusCode [%d] but was [%d] no content",
                                200, statusCode));
            }
        }

        assertHttpResponseHasHeaders(httpResponse);
        assertHttpResponseHasHeader(httpResponse, HttpHeader.CONTENT_TYPE);
        assertHttpResponseHasBody(httpResponse);
    }
}
