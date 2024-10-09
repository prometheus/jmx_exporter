/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prometheus.jmx;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.security.auth.UserPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.security.auth.Subject;

public class CustomAuthenticator extends Authenticator {

    @Override
    public Result authenticate(HttpExchange exch) {
        // nothing too custom, so the test works, just to demonstrate that it is plug-able
        Headers rmap = exch.getRequestHeaders();
        String auth = rmap.getFirst("Authorization");
        if (auth == null) {
            return new Authenticator.Retry(401);
        }
        int sp = auth.indexOf(' ');
        if (sp == -1 || !auth.substring(0, sp).equals("Basic")) {
            return new Authenticator.Failure(401);
        }
        byte[] b = Base64.getDecoder().decode(auth.substring(sp + 1));
        String userpass = new String(b, StandardCharsets.UTF_8);
        int colon = userpass.indexOf(':');
        String uname = userpass.substring(0, colon);
        String pass = userpass.substring(colon + 1);

        if ("Prometheus".equals(uname) && "secret".equals(pass)) {
            Subject subject = new Subject();
            subject.getPrincipals().add(new UserPrincipal(uname));
            // to communicate an authenticated subject for subsequent handler calls via Subject.doAs
            exch.setAttribute("io.prometheus.jmx.CustomAuthenticatorSubjectAttribute", subject);
            return new Authenticator.Success(new HttpPrincipal(uname, "/"));
        } else {
            return new Authenticator.Failure(401);
        }
    }
}
