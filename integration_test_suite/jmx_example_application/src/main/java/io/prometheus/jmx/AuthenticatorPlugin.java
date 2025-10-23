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

/** Example custom authenticator */
public class AuthenticatorPlugin extends Authenticator {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC = "Basic";

    private static final String USERNAME = "Prometheus";
    private static final String PASSWORD = "secret";

    /** Constructor */
    public AuthenticatorPlugin() {
        // INTENTIONALLY BLANK
    }

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

        byte[] usernamePasswordBytes =
                Base64.getDecoder().decode(authorization.substring(space + 1));
        String usernamePassword = new String(usernamePasswordBytes, StandardCharsets.UTF_8);
        int colon = usernamePassword.indexOf(':');
        String username = usernamePassword.substring(0, colon);
        String password = usernamePassword.substring(colon + 1);

        if (USERNAME.equals(username) && PASSWORD.equals(password)) {
            Subject subject = new Subject();
            subject.getPrincipals().add(new UserPrincipal(username));
            // to communicate an authenticated subject for subsequent handler calls via Subject.doAs
            httpExchange.setAttribute(
                    "io.prometheus.jmx.CustomAuthenticatorSubjectAttribute", subject);
            return new Authenticator.Success(new HttpPrincipal(username, "/"));
        } else {
            return new Authenticator.Failure(401);
        }
    }

    /**
     * Class to implement Principal
     *
     * <p>Required for ibmjava:8 since it doesn't provide com.sun.security.auth.UserPrincipal
     */
    public static class UserPrincipal implements Principal, Serializable {

        private final String name;

        /**
         * Constructor
         *
         * @param name name
         */
        public UserPrincipal(String name) {
            this.name = Objects.requireNonNull(name, "Name cannot be null");
        }

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
