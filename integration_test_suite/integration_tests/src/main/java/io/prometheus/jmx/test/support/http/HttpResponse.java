package io.prometheus.jmx.test.support.http;

import java.io.IOException;
import java.util.function.Consumer;
import okhttp3.Headers;
import okhttp3.ResponseBody;

public class HttpResponse {

    public static final int OK = 200;
    public static final int UNAUTHORIZED = 401;

    private final int code;
    private final Headers headers;
    private final HttpResponseBody body;

    public HttpResponse(okhttp3.Response response) throws IOException {
        this.code = response.code();
        this.headers = response.headers();

        try {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                body = new HttpResponseBody(responseBody.bytes());
            } else {
                body = null;
            }
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Throwable t) {
                    // DO NOTHING
                }
            }
        }
    }

    public int statusCode() {
        return code;
    }

    public Headers headers() {
        return headers;
    }

    public HttpResponseBody body() {
        return body;
    }

    public HttpResponse accept(Consumer<HttpResponse> consumer) {
        consumer.accept(this);
        return this;
    }
}
