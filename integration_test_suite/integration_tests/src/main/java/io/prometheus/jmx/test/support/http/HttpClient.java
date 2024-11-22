/*
 * Copyright (C) 2024-present The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.support.http;

import io.prometheus.jmx.test.support.SSLContextException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** Class to implement HttpClient */
public class HttpClient {

    /** Default connect timeout */
    public static final int CONNECT_TIMEOUT = 30000;

    /** Default read timeout */
    public static final int READ_TIMEOUT = 30000;

    static {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    null,
                    new TrustManager[] {new TrustAllCertificates()},
                    new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new SSLContextException(
                    "Failed to initialize SSL context for self-signed certificates", e);
        }
    }

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
        URL url = new URL(httpRequest.url());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        HttpRequest.Method method = httpRequest.method();
        connection.setRequestMethod(method.toString());

        for (Map.Entry<String, List<String>> header : httpRequest.headers().entrySet()) {
            for (String value : header.getValue()) {
                connection.addRequestProperty(header.getKey(), value);
            }
        }

        if (method == HttpRequest.Method.PUT || method == HttpRequest.Method.POST) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = httpRequest.body().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        int status = connection.getResponseCode();
        String message = connection.getResponseMessage();

        Map<String, List<String>> headerFields = connection.getHeaderFields();
        Map<String, List<String>> headers = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            if (entry.getKey() != null) {
                headers.put(entry.getKey().toUpperCase(Locale.US), entry.getValue());
            }
        }

        byte[] body;
        try (InputStream inputStream =
                (status > 299) ? connection.getErrorStream() : connection.getInputStream()) {
            if (inputStream == null) {
                body = null;
            } else {
                body = readBytes(inputStream);
            }
        }

        return new HttpResponse(status, message, headers, body);
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

    /** Class to implement TrustAllCertificates */
    private static class TrustAllCertificates implements X509TrustManager {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    }
}
