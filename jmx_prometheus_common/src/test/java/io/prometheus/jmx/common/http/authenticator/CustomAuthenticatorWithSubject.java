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

package io.prometheus.jmx.common.http.authenticator;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import javax.security.auth.Subject;

public class CustomAuthenticatorWithSubject extends Authenticator {
    final String authenticatedUser = "guest";

    @Override
    public Result authenticate(HttpExchange exch) {
        Subject subject = new Subject();
        subject.getPrincipals().add(() -> authenticatedUser);
        exch.setAttribute("custom.subject", subject);
        return new Success(new HttpPrincipal(authenticatedUser, "/"));
    }
}
