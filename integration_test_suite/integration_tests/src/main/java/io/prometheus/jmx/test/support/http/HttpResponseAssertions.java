package io.prometheus.jmx.test.support.http;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpResponseAssertions {

    private HttpResponseAssertions() {
        // DO NOTHING
    }

    public static void assertHttpResponseCode(HttpResponse httpResponse, int code) {
        assertThat(httpResponse.code()).isEqualTo(code);
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
        assertThat(httpResponse.code()).isEqualTo(200);
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().string().length()).isGreaterThan(0);
        assertThat(httpResponse.body().string()).isEqualTo("Exporter is healthy.\n");
    }

    public static void assertHttpMetricsResponse(HttpResponse httpResponse) {
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.code()).isEqualTo(200);
        assertHttpResponseHasHeaders(httpResponse);
        assertHttpResponseHasHeader(httpResponse, HttpHeader.CONTENT_TYPE);
        assertHttpResponseHasBody(httpResponse);
    }
}
