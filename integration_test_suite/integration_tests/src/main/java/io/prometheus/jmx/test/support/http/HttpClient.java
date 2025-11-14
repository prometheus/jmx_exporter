/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.support.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import nl.altindag.ssl.SSLFactory;

/** Class to implement HttpClient */
public class HttpClient {

    /** Default connect timeout */
    public static final int CONNECT_TIMEOUT = 60000;

    /** Default read timeout */
    public static final int READ_TIMEOUT = 60000;

    private static final SSLContext UNSAFE_SSLCONTEXT =
            SSLFactory.builder().withUnsafeTrustMaterial().build().getSslContext();
    private static final java.net.http.HttpClient defaultHttpClient =
            createHttpClient(CONNECT_TIMEOUT, READ_TIMEOUT, UNSAFE_SSLCONTEXT);

    /**
     * Send an HTTP request
     *
     * @param url url
     * @return an HttpResponse
     * @throws IOException IOException
     */
    public static HttpResponse sendRequest(String url) throws IOException {
        return sendRequest(HttpRequest.builder().url(url).build());
    }

    public static HttpResponse sendRequest(String url, SSLContext sslContext) throws IOException {
        return sendRequest(
                HttpRequest.builder().url(url).build(), CONNECT_TIMEOUT, READ_TIMEOUT, sslContext);
    }

    /**
     * Send an HTTP request with a single header
     *
     * @param url url
     * @param header header
     * @param value value
     * @return an HttpResponse
     * @throws IOException IOException
     */
    public static HttpResponse sendRequest(String url, String header, String value)
            throws IOException {
        return sendRequest(HttpRequest.builder().url(url).header(header, value).build());
    }

    /**
     * Send an HTTP request with a single header
     *
     * @param url url
     * @param header header
     * @param value value
     * @return an HttpResponse
     * @throws IOException IOException
     */
    public static HttpResponse sendRequest(
            String url, String header, String value, SSLContext sslContext) throws IOException {
        return sendRequest(
                HttpRequest.builder().url(url).header(header, value).build(),
                CONNECT_TIMEOUT,
                READ_TIMEOUT,
                sslContext);
    }

    /**
     * Send an HTTP request with a Map of headers
     *
     * @param url url
     * @param headers headers
     * @return an HttpResponse
     * @throws IOException IOException
     */
    public static HttpResponse sendRequest(String url, Map<String, Collection<String>> headers)
            throws IOException {
        return sendRequest(HttpRequest.builder().url(url).headers(headers).build());
    }

    /**
     * Send an HttpRequest
     *
     * @param httpRequest httpRequest
     * @return an HttpResponse
     * @throws IOException IOException
     */
    public static HttpResponse sendRequest(HttpRequest httpRequest) throws IOException {
        return sendRequest(httpRequest, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Send an HttpRequest
     *
     * @param httpRequest httpRequest
     * @param connectTimeout connectTimeout
     * @param readTimeout readTimeout
     * @return an HttpResponse
     * @throws IOException IOException
     */
    public static HttpResponse sendRequest(
            HttpRequest httpRequest, int connectTimeout, int readTimeout) throws IOException {
        return sendRequest(httpRequest, connectTimeout, readTimeout, null);
    }

    /**
     * Send an HttpRequest
     *
     * @param httpRequest httpRequest
     * @param connectTimeout connectTimeout
     * @param readTimeout readTimeout
     * @param sslContext sslContext
     * @return an HttpResponse
     * @throws IOException IOException
     */
    public static HttpResponse sendRequest(
            HttpRequest httpRequest, int connectTimeout, int readTimeout, SSLContext sslContext)
            throws IOException {

        java.net.http.HttpClient httpClient =
                getHttpClient(connectTimeout, readTimeout, sslContext);

        HttpRequest.Method method = httpRequest.method();
        java.net.http.HttpRequest.Builder requestBuilder =
                java.net.http.HttpRequest.newBuilder().uri(URI.create(httpRequest.url()));
        switch (method) {
            case GET:
                requestBuilder = requestBuilder.GET();
                break;
            case POST:
                requestBuilder =
                        requestBuilder.POST(
                                java.net.http.HttpRequest.BodyPublishers.ofString(
                                        httpRequest.body(), StandardCharsets.UTF_8));
                break;
            case PUT:
                requestBuilder =
                        requestBuilder.PUT(
                                java.net.http.HttpRequest.BodyPublishers.ofString(
                                        httpRequest.body(), StandardCharsets.UTF_8));
                break;
        }

        for (Map.Entry<String, List<String>> header : httpRequest.headers().entrySet()) {
            for (String value : header.getValue()) {
                requestBuilder = requestBuilder.header(header.getKey(), value);
            }
        }

        java.net.http.HttpRequest request = requestBuilder.build();

        java.net.http.HttpResponse<InputStream> response;
        try {
            response =
                    httpClient.send(
                            request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        int status = response.statusCode();

        byte[] body;
        try (InputStream inputStream = response.body()) {
            if (inputStream == null) {
                body = null;
            } else {
                body = readBytes(inputStream);
            }
        }

        return new HttpResponse(status, "hello", response.headers().map(), body);
    }

    /**
     * Method to read all bytes from an InputStream
     *
     * @param inputStream inputStream
     * @return a byte array
     * @throws IOException IOException
     */
    private static byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[10240];
        int bytesRead;

        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
            buffer.flush();
        }

        return buffer.toByteArray();
    }

    private static java.net.http.HttpClient getHttpClient(
            int connectTimeout, int readTimeout, SSLContext sslContext) {
        if (connectTimeout != CONNECT_TIMEOUT || sslContext != null) {
            return createHttpClient(
                    connectTimeout,
                    readTimeout,
                    sslContext != null ? sslContext : UNSAFE_SSLCONTEXT);
        }
        return defaultHttpClient;
    }

    private static java.net.http.HttpClient createHttpClient(
            int connectTimeout, int readTimeout, SSLContext sslContext) {

        if (defaultHttpClient == null) {
            System.setProperty(
                    "jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
        }

        return java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.of(connectTimeout, TimeUnit.MILLISECONDS.toChronoUnit()))
                .sslContext(sslContext)
                .build();
    }
}
