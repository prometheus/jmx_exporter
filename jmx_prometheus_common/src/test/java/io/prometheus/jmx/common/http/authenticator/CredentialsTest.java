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

package io.prometheus.jmx.common.http.authenticator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.prometheus.jmx.common.authenticator.Credentials;
import org.junit.jupiter.api.Test;

public class CredentialsTest {

    @Test
    public void testEquals() {
        String username = "Prometheus";
        String password = "secret";

        Credentials credentials1 = new Credentials(username, password);
        Credentials credentials2 = new Credentials(username, password);

        assertThat(credentials1).isEqualTo(credentials2);
    }

    @Test
    public void testNotEquals() {
        String username = "Prometheus";
        String password = "secret";

        Credentials credentials1 = new Credentials(username, password);
        Credentials credentials2 = new Credentials(username, password + "X");

        assertThat(credentials1).isNotEqualTo(credentials2);
    }

    @Test
    public void testNotEqualsDifferentUsername() {
        Credentials credentials1 = new Credentials("Prometheus", "secret");
        Credentials credentials2 = new Credentials("prometheus", "secret");

        assertThat(credentials1).isNotEqualTo(credentials2);
    }

    @Test
    public void testNotEqualsNull() {
        Credentials credentials = new Credentials("Prometheus", "secret");

        assertThat(credentials).isNotEqualTo(null);
    }

    @Test
    public void testNotEqualsDifferentType() {
        Credentials credentials = new Credentials("Prometheus", "secret");

        assertThat(credentials).isNotEqualTo("Prometheussecret");
    }

    @Test
    public void testEqualsSameInstance() {
        Credentials credentials = new Credentials("Prometheus", "secret");

        assertThat(credentials).isEqualTo(credentials);
    }

    @Test
    public void testHashCode() {
        String username = "Prometheus";
        String password = "secret";

        Credentials credentials1 = new Credentials(username, password);
        Credentials credentials2 = new Credentials(username, password);

        assertThat(credentials1.hashCode()).isEqualTo(credentials2.hashCode());
    }

    @Test
    public void testConstantTimeComparisonSamePrefix() {
        Credentials credentials1 = new Credentials("Prometheus", "secret");
        Credentials credentials2 = new Credentials("Prometheus", "secretX");
        Credentials credentials3 = new Credentials("Prometheus", "secretY");

        assertThat(credentials1).isNotEqualTo(credentials2);
        assertThat(credentials1).isNotEqualTo(credentials3);
        assertThat(credentials2).isNotEqualTo(credentials3);
    }

    @Test
    public void testConstantTimeComparisonSingleCharDifference() {
        Credentials credentials1 = new Credentials("user", "passwordA");
        Credentials credentials2 = new Credentials("user", "passwordB");

        assertThat(credentials1).isNotEqualTo(credentials2);
    }
}
