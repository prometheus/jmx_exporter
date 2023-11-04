package io.prometheus.jmx.test.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import okhttp3.Headers;
import okhttp3.ResponseBody;

public class Response {

    public static final int OK = 200;
    public static final int UNAUTHORIZED = 401;

    private final int code;
    private final Headers headers;
    private final byte[] body;

    public Response(okhttp3.Response response) throws IOException {
        this.code = response.code();
        this.headers = response.headers();

        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            this.body = responseBody.bytes();
        } else {
            this.body = null;
        }
    }

    public int code() {
        return code;
    }

    public Headers headers() {
        return headers;
    }

    public byte[] body() {
        return body;
    }

    public String string() {
        if (body != null) {
            return new String(body, StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }
}
