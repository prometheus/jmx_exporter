package io.prometheus.jmx.test.support;

import io.prometheus.jmx.test.support.Response;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponseAssertions {

    private ResponseAssertions() {
        // DO NOTHING
    }

    public static void assertOk(Response response) {
        assertThat(response.code()).isEqualTo(200);
    }

    public static void assertUnauthorized(Response response) {
        assertThat(response.code()).isEqualTo(401);
    }

    public static void assertHasBody(Response response) {
        assertThat(response.body()).isNotNull();
        assertThat(response.body().length).isGreaterThan(0);
    }

    public static void assertHealthyResponse(Response response) {
        assertOk(response);
        assertThat(response.body()).isNotNull();
        assertThat(response.body().length).isGreaterThan(0);

        String body = new String(response.body(), StandardCharsets.UTF_8);
        assertThat(body).isEqualTo("Exporter is healthy.\n");
    }
}
