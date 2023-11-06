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

import okhttp3.Request;

/** Class to implement Basic authentication credentials */
public class HttpBasicAuthenticationCredentials implements HttpCredentials {

    private final String username;
    private final String password;

    /**
     * Constructor
     *
     * @param username username
     * @param password password
     */
    public HttpBasicAuthenticationCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Method to apply the Credentials to a Request.Builder
     *
     * @param requestBuilder requestBuilder
     */
    public void apply(Request.Builder requestBuilder) {
        if ((username != null) && (password != null)) {
            requestBuilder.addHeader(
                    HttpHeader.AUTHORIZATION, HttpClient.basicAuthentication(username, password));
        }
    }
}
