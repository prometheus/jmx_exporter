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

import java.io.IOException;
import java.net.UnknownServiceException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.HostnameVerifierUtils;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Class to implement HttpClient */
public class HttpClient {

    /** Default connect timeout in milliseconds */
    public static final int CONNECT_TIMEOUT = 60000;

    /** Default read timeout in milliseconds */
    public static final int WRITE_TIMEOUT = 60000;

    /** Default read timeout in milliseconds */
    public static final int READ_TIMEOUT = 60000;

    /** Default maximum total connections */
    public static final int MAXIMUM_CONNECTIONS = 200;

    /** Default eviction timeout in seconds */
    public static final int EVICTION_TIMEOUT = 30;

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");
    private static final MediaType TEXT_MEDIA_TYPE = MediaType.parse("text/plain; charset=utf-8");

    private static final SSLFactory UNSAFE_SSL_FACTORY =
            SSLFactory.builder().withUnsafeTrustMaterial().build();

    private static final OkHttpClient defaultHttpClient =
            createHttpClient(
                    CONNECT_TIMEOUT,
                    WRITE_TIMEOUT,
                    READ_TIMEOUT,
                    UNSAFE_SSL_FACTORY.getSslContext());

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

    /**
     * Send an HTTP request using a specified SSL context
     *
     * @param url url
     * @param sslContext sslContext
     * @return an HttpResponse
     * @throws IOException IOException
     */
    public static HttpResponse sendRequest(String url, SSLContext sslContext) throws IOException {
        return sendRequest(
                HttpRequest.builder().url(url).build(),
                CONNECT_TIMEOUT,
                WRITE_TIMEOUT,
                READ_TIMEOUT,
                sslContext);
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
                WRITE_TIMEOUT,
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
        return sendRequest(httpRequest, CONNECT_TIMEOUT, WRITE_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Send an HttpRequest
     *
     * @param httpRequest httpRequest
     * @param connectTimeout connectTimeout
     * @param writeTimeout writeTimeout
     * @param readTimeout readTimeout
     * @return an HttpResponse
     * @throws IOException IOException
     */
    public static HttpResponse sendRequest(
            HttpRequest httpRequest, int connectTimeout, int writeTimeout, int readTimeout)
            throws IOException {
        return sendRequest(httpRequest, connectTimeout, writeTimeout, readTimeout, null);
    }

    /**
     * Send an HttpRequest
     *
     * @param httpRequest httpRequest
     * @param connectTimeout connectTimeout
     * @param writeTimeout writeTimeout
     * @param readTimeout readTimeout
     * @param sslContext sslContext
     * @return an HttpResponse
     * @throws IOException IOException
     */
    public static HttpResponse sendRequest(
            HttpRequest httpRequest,
            int connectTimeout,
            int writeTimeout,
            int readTimeout,
            SSLContext sslContext)
            throws IOException {

        OkHttpClient httpClient =
                getHttpClient(connectTimeout, writeTimeout, readTimeout, sslContext);

        HttpRequest.Method method = httpRequest.method();
        Request.Builder requestBuilder = new Request.Builder().url(httpRequest.url());

        // Add headers
        for (Map.Entry<String, List<String>> header : httpRequest.headers().entrySet()) {
            for (String value : header.getValue()) {
                requestBuilder.addHeader(header.getKey(), value);
            }
        }

        // Set method and body
        RequestBody body = null;
        if (method == HttpRequest.Method.POST || method == HttpRequest.Method.PUT) {
            String bodyContent = httpRequest.body();
            if (bodyContent != null && !bodyContent.isEmpty()) {
                body = RequestBody.create(bodyContent, TEXT_MEDIA_TYPE);
            } else {
                body = RequestBody.create("", TEXT_MEDIA_TYPE);
            }
        }

        switch (method) {
            case GET:
                {
                    requestBuilder.get();
                    break;
                }
            case POST:
                {
                    requestBuilder.post(body);
                    break;
                }
            case PUT:
                {
                    requestBuilder.put(body);
                    break;
                }
        }

        Request request = requestBuilder.build();

        // Execute request with automatic retry handling (OkHttp handles retries internally)
        try (Response response = httpClient.newCall(request).execute()) {
            int status = response.code();
            String message = response.message();

            Map<String, List<String>> headers = new HashMap<>();
            for (String headerName : response.headers().names()) {
                headers.put(
                        headerName.toUpperCase(Locale.US),
                        Collections.singletonList(response.header(headerName)));
            }

            byte[] responseBody = null;
            ResponseBody responseBodyObj = response.body();
            if (responseBodyObj != null) {
                responseBody = responseBodyObj.bytes();
            }

            return new HttpResponse(status, message, headers, responseBody);
        } catch (UnknownServiceException e) {
            // Wrap UnknownServiceException as SSLHandshakeException for API compatibility
            // OkHttp throws UnknownServiceException when cipher suites don't match,
            // but Apache HttpClient threw SSLHandshakeException
            throw new SSLHandshakeException("SSL handshake failed: " + e.getMessage());
        }
    }

    /**
     * Get or create an OkHttpClient
     *
     * @param connectTimeout connectTimeout
     * @param writeTimeout writeTimeout
     * @param readTimeout readTimeout
     * @param sslContext sslContext
     * @return OkHttpClient
     */
    private static OkHttpClient getHttpClient(
            int connectTimeout, int writeTimeout, int readTimeout, SSLContext sslContext) {
        if (connectTimeout != CONNECT_TIMEOUT
                || writeTimeout != WRITE_TIMEOUT
                || readTimeout != READ_TIMEOUT
                || sslContext != null) {
            return createHttpClient(
                    connectTimeout,
                    writeTimeout,
                    readTimeout,
                    sslContext != null ? sslContext : UNSAFE_SSL_FACTORY.getSslContext());
        }

        return defaultHttpClient;
    }

    /**
     * Create an OkHttpClient with improved connection management
     *
     * @param connectTimeout connectTimeout
     * @param writeTimeout writeTimeout
     * @param readTimeout readTimeout
     * @param sslContext sslContext
     * @return OkHttpClient
     */
    private static OkHttpClient createHttpClient(
            int connectTimeout, int writeTimeout, int readTimeout, SSLContext sslContext) {

        SSLSocketFactory sslSocketFactory;
        X509TrustManager trustManager;

        // Use SSLFactory only for the default unsafe context
        if (sslContext == UNSAFE_SSL_FACTORY.getSslContext()) {
            sslSocketFactory = UNSAFE_SSL_FACTORY.getSslSocketFactory();
            trustManager =
                    UNSAFE_SSL_FACTORY
                            .getTrustManager()
                            .orElseThrow(
                                    () -> new IllegalStateException("TrustManager not available"));
        } else {
            // For custom SSLContext, extract socket factory and use unsafe trust manager
            sslSocketFactory = sslContext.getSocketFactory();

            // Reuse the unsafe trust manager from the default SSL factory
            // This matches the behavior of the original code which used unsafe trust material
            trustManager =
                    UNSAFE_SSL_FACTORY
                            .getTrustManager()
                            .orElseThrow(
                                    () -> new IllegalStateException("TrustManager not available"));
        }

        HostnameVerifier hostnameVerifier = HostnameVerifierUtils.createUnsafe();

        // Connection pool with idle connection eviction
        ConnectionPool connectionPool =
                new ConnectionPool(MAXIMUM_CONNECTIONS, EVICTION_TIMEOUT, TimeUnit.SECONDS);

        // Use a compatible connection spec that will accept whatever the SSLContext provides
        // This prevents UnknownServiceException when custom SSLContext has restricted cipher suites
        List<ConnectionSpec> connectionSpecs =
                Arrays.asList(
                        ConnectionSpec.MODERN_TLS,
                        ConnectionSpec.COMPATIBLE_TLS,
                        ConnectionSpec.CLEARTEXT);

        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .callTimeout(connectTimeout + writeTimeout + readTimeout, TimeUnit.MILLISECONDS)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier(hostnameVerifier)
                .connectionSpecs(connectionSpecs)
                .connectionPool(connectionPool)
                .retryOnConnectionFailure(true)
                .build();
    }
}
