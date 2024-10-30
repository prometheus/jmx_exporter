package io.prometheus.jmx.test.support.http;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Class to implement HttpResponse */
public class HttpResponse {

    private final int statusCode;
    private final String statusMessage;
    private final HttpResponseBody body;
    private final Map<String, List<String>> headers;

    /**
     * Constructor
     *
     * @param statusCode statusCode
     * @param statusMessage statusMessage
     * @param headers headers
     * @param body body
     */
    public HttpResponse(
            int statusCode, String statusMessage, Map<String, List<String>> headers, byte[] body) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.headers = headers;
        this.body = new HttpResponseBody(body);
    }

    /**
     * Get the status code
     *
     * @return the status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Get the status message
     *
     * @return the status message
     */
    public String statusMessage() {
        return statusMessage;
    }

    /**
     * Get the Map of headers
     *
     * @return a Map of headers
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Get a List of header values
     *
     * @param name name
     * @return a List of header values
     */
    public List<String> header(String name) {
        return headers.get(name.toUpperCase(Locale.US));
    }

    /**
     * Get the body
     *
     * @return the body
     */
    public HttpResponseBody body() {
        return body;
    }
}
