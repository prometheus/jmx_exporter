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

import java.util.Objects;

/** Class to implement credentials */
public class Credentials {

    private final String username;
    private final String password;

    /**
     * Constructor
     *
     * @param username username
     * @param password password
     */
    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Method to get the size (username length + password length) of the credentials
     *
     * @return the size of the credentials
     */
    public int size() {
        return username.length() + password.length();
    }

    @Override
    public String toString() {
        return username + password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Credentials Credentials = (Credentials) o;
        return Objects.equals(username, Credentials.username)
                && Objects.equals(password, Credentials.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}
