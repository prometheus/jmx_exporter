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

package io.prometheus.jmx.common.http.authenticator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CredentialTest {

    @Test
    public void testEquals() {
        String username = "Prometheus";
        String password = "secret";

        Credential credential1 = new Credential(username, password);
        Credential credential2 = new Credential(username, password);

        assertTrue(credential1.equals(credential2));
    }

    @Test
    public void testList() {
        String username = "Prometheus";
        String password = "secret";

        Credential credential1 = new Credential(username, password);
        Credential credential2 = new Credential(username, password + "X");

        assertFalse(credential1.equals(credential2));
    }
}
