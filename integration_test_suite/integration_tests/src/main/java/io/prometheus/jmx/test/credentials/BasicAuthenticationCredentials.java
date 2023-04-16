/*
 * Copyright 2023 Douglas Hoard
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

package io.prometheus.jmx.test.credentials;

import io.prometheus.jmx.test.HttpHeader;
import okhttp3.Request;

import static io.prometheus.jmx.test.HttpClient.basicAuthentication;

public class BasicAuthenticationCredentials implements Credentials {

    private static final BasicAuthenticationCredentials USERNAME__PASSWORD = new BasicAuthenticationCredentials("prometheus", "secret");
    private static final BasicAuthenticationCredentials USERNAME__BAD_PASSWORD = new BasicAuthenticationCredentials("prometheus", "bad");
    private static final BasicAuthenticationCredentials USERNAME__NULL_PASSWORD = new BasicAuthenticationCredentials("prometheus", null);

    private static final BasicAuthenticationCredentials BAD_USERNAME__PASSWORD = new BasicAuthenticationCredentials("bad", "secret");
    private static final BasicAuthenticationCredentials BAD_USERNAME__BAD_PASSWORD = new BasicAuthenticationCredentials("bad", "bad");
    private static final BasicAuthenticationCredentials BAD_USERNAME__NULL_PASSWORD = new BasicAuthenticationCredentials("bad", null);

    private static final BasicAuthenticationCredentials NULL_USERNAME__PASSWORD = new BasicAuthenticationCredentials(null, "secret");
    private static final BasicAuthenticationCredentials NULL_USERNAME__NULL_PASSWORD = new BasicAuthenticationCredentials(null, null);


    private final String username;
    private final String password;

    public BasicAuthenticationCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void apply(Request.Builder requestBuilder) {
        if ((username != null) && (password != null)) {
            requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(username, password));
        }
    }
}
