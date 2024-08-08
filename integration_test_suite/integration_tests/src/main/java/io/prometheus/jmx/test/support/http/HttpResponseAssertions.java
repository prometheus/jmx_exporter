package io.prometheus.jmx.test.support.http;

import static java.lang.String.format;
import static org.antublue.verifyica.api.Fail.fail;
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
                fail(
                        format(
                                "Exporter error, HTTP status code [%d] content [%n%s]",
                                statusCode, content));
            } else {
                fail(format("Exporter error, HTTP status code [%d] no content", statusCode));
            }
        }

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertHttpResponseHasHeaders(httpResponse);
        assertHttpResponseHasHeader(httpResponse, HttpHeader.CONTENT_TYPE);
        assertHttpResponseHasBody(httpResponse);
    }
}
