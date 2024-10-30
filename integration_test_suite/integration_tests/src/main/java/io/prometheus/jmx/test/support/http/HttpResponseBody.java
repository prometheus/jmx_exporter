package io.prometheus.jmx.test.support.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** Class to implement HttpResponseBody */
public class HttpResponseBody {

    private final byte[] bytes;

    /**
     * Constructor
     *
     * @param bytes bytes
     */
    public HttpResponseBody(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Get the body byte array
     *
     * @return the body byte array
     */
    public byte[] bytes() {
        return bytes;
    }

    /**
     * Get the body as a String
     *
     * @return the body as a String
     */
    public String string() {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Get the body as a String using a specific Charset
     *
     * @param charset charset
     * @return the body as a String using a specific Charset
     */
    public String string(Charset charset) {
        return new String(bytes, charset);
    }
}
