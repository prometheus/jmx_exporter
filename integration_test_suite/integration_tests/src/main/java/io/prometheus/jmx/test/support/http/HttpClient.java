/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
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

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpClient {

    private static OkHttpClient OK_HTTP_CLIENT;

    private final String baseUrl;

    public HttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public HttpResponse send(HttpRequest httpRequest) {
        return httpRequest.send(this);
    }

    public void send(HttpRequest httpRequest, Consumer<HttpResponse> httpResponseConsumer) {
        httpResponseConsumer.accept(httpRequest.send(this));
    }

    public Request.Builder createRequest(String path) {
        return new Request.Builder().url(baseUrl + path);
    }

    public Response execute(Request request) throws Exception {
        synchronized (this) {
            if (OK_HTTP_CLIENT == null) {
                OK_HTTP_CLIENT = createOkHttpClient();
            }
        }

        return OK_HTTP_CLIENT.newCall(request).execute();
    }

    public Response execute(Request.Builder requestBuilder) throws Exception {
        return execute(requestBuilder.build());
    }

    public static void close(Response response) {
        if (response != null) {
            response.close();
        }
    }

    public static String basicAuthentication(String username, String password) {
        return String.format(
                "Basic %s",
                Base64.getEncoder()
                        .encodeToString(
                                (username + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }

    private static OkHttpClient createOkHttpClient() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustAllManager()};
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        okHttpClientBuilder.hostnameVerifier(new TrustHostnameVerifier());

        // Increase timeouts for slow test machines
        okHttpClientBuilder.connectTimeout(Duration.ofSeconds(30));
        okHttpClientBuilder.readTimeout(Duration.ofSeconds(30));
        okHttpClientBuilder.writeTimeout(Duration.ofSeconds(30));

        // Set the thread pool
        okHttpClientBuilder.connectionPool(
                new ConnectionPool(
                        Runtime.getRuntime().availableProcessors() + 5, 5L, TimeUnit.MINUTES));

        return okHttpClientBuilder.build();
    }

    /** Trust manager that accepts all certificates */
    static class X509TrustAllManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // DO NOTHING
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // DO NOTHING
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /** Hostname verifier that accepts all hostnames */
    static class TrustHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}
