/*
 * Copyright (C) 2022-2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.common.http.authenticator;

import com.sun.net.httpserver.BasicAuthenticator;
import io.prometheus.jmx.common.util.Precondition;

/** Class to implement a username / plaintext password BasicAuthenticator */
public class PlaintextAuthenticator extends BasicAuthenticator {

    private final String username;
    private final String password;

    /**
     * Constructor
     *
     * @param realm realm
     * @param username username
     * @param password password
     */
    public PlaintextAuthenticator(String realm, String username, String password) {
        super(realm);

        Precondition.notNullOrEmpty(username);
        Precondition.notNullOrEmpty(password);

        this.username = username;
        this.password = password;
    }

    /**
     * called for each incoming request to verify the given name and password in the context of this
     * Authenticator's realm. Any caching of credentials must be done by the implementation of this
     * method
     *
     * @param username the username from the request
     * @param password the password from the request
     * @return <code>true</code> if the credentials are valid, <code>false</code> otherwise.
     */
    @Override
    public boolean checkCredentials(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }
}
