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

package io.prometheus.jmx;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Objects;
import javax.security.auth.Subject;

/**
 * HTTP Basic authenticator for the JMX exporter, validating credentials against a fixed username
 * and password.
 *
 * <p>On successful authentication, the authenticated {@link Subject} is stored as a request
 * attribute under the key {@code io.prometheus.jmx.CustomAuthenticatorSubjectAttribute} for
 * subsequent handler access via {@link Subject#doAs}.
 *
 * <p>This class is designed as a pluggable authenticator for integration testing of the
 * JMX exporter HTTP server authentication mechanism.
 */
public class AuthenticatorPlugin extends Authenticator {

    /**
     * HTTP {@code Authorization} header name.
     */
    private static final String AUTHORIZATION = "Authorization";

    /**
     * HTTP Basic authentication scheme name.
     */
    private static final String BASIC = "Basic";

    /**
     * Accepted username for authentication.
     */
    private static final String USERNAME = "Prometheus";

    /**
     * Accepted password for authentication.
     */
    private static final String PASSWORD = "secret";

    /**
     * Constructs a new instance.
     */
    public AuthenticatorPlugin() {
        // INTENTIONALLY BLANK
    }

    /**
     * Authenticates the incoming HTTP request using HTTP Basic authentication.
     *
     * <p>Returns {@link Success} when credentials match the configured username and password,
     * {@link Retry} when the {@code Authorization} header is missing, or {@link Failure} when
     * credentials are invalid or the scheme is not {@code Basic}.
     *
     * @param httpExchange the HTTP exchange containing the request headers
     * @return the authentication result indicating success, retry, or failure
     */
    @Override
    public Result authenticate(HttpExchange httpExchange) {
        // nothing too custom, so the test works, just to demonstrate that it is plug-able
        Headers headers = httpExchange.getRequestHeaders();

        String authorization = headers.getFirst(AUTHORIZATION);
        if (authorization == null) {
            return new Authenticator.Retry(401);
        }

        int space = authorization.indexOf(' ');
        if (space == -1 || !authorization.substring(0, space).equals(BASIC)) {
            return new Authenticator.Failure(401);
        }

        byte[] usernamePasswordBytes = Base64.getDecoder().decode(authorization.substring(space + 1));
        String usernamePassword = new String(usernamePasswordBytes, StandardCharsets.UTF_8);
        int colon = usernamePassword.indexOf(':');
        String username = usernamePassword.substring(0, colon);
        String password = usernamePassword.substring(colon + 1);

        if (USERNAME.equals(username) && PASSWORD.equals(password)) {
            Subject subject = new Subject();
            subject.getPrincipals().add(new UserPrincipal(username));
            // to communicate an authenticated subject for subsequent handler calls via Subject.doAs
            httpExchange.setAttribute("io.prometheus.jmx.CustomAuthenticatorSubjectAttribute", subject);
            return new Authenticator.Success(new HttpPrincipal(username, "/"));
        } else {
            return new Authenticator.Failure(401);
        }
    }

    /**
     * Simple {@link Principal} implementation representing an authenticated user by name.
     *
     * <p>Required for IBM Java 8, which does not provide {@code com.sun.security.auth.UserPrincipal}.
     */
    public static class UserPrincipal implements Principal, Serializable {

        /**
         * The authenticated user name.
         */
        private final String name;

        /**
         * Constructs a new user principal with the given name.
         *
         * @param name the user name; must not be {@code null}
         * @throws NullPointerException if {@code name} is {@code null}
         */
        public UserPrincipal(String name) {
            this.name = Objects.requireNonNull(name, "Name cannot be null");
        }

        /**
         * Returns the user name.
         *
         * @return the authenticated user name
         */
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "UserPrincipal{" + "name='" + name + '\'' + '}';
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            UserPrincipal that = (UserPrincipal) object;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }
}
