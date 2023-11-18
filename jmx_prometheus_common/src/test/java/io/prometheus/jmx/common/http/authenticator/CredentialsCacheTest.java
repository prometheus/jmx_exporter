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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class CredentialCacheTest {

    @Test
    public void test1() {
        String username = "prometheus";
        String password = "secret";
        Credential credential = new Credential(username, password);
        int credentialSizeBytes = credential.toString().getBytes(StandardCharsets.UTF_8).length;

        CredentialCache credentialCache = new CredentialCache(credentialSizeBytes);

        credentialCache.add(credential);

        assertThat(credentialCache.contains(credential)).isTrue();
        assertThat(credentialCache.contains(new Credential(username, password))).isTrue();
        assertThat(credentialCache.getCurrentCacheSizeBytes()).isEqualTo(credentialSizeBytes);
        assertThat(credentialCache.getCurrentCacheSizeBytes())
                .isEqualTo(credentialCache.getMaximumCacheSizeBytes());

        assertThat(credentialCache.remove(new Credential(username, password))).isTrue();
        assertThat(credentialCache.contains(new Credential(username, password))).isFalse();
    }

    @Test
    public void test2() {
        String username = "prometheus";
        String password = "secret";
        Credential credential = new Credential(username + "X", password);
        int credentialSizeBytes = credential.toString().getBytes(StandardCharsets.UTF_8).length;

        CredentialCache credentialCache = new CredentialCache(credentialSizeBytes);

        for (int i = 0; i < 10; i++) {
            credential = new Credential(username + i, password);

            credentialCache.add(credential);

            assertThat(credentialCache.contains(credential)).isTrue();
            assertThat(credentialCache.contains(new Credential(username + i, password))).isTrue();
            assertThat(credentialCache.getCurrentCacheSizeBytes()).isEqualTo(credentialSizeBytes);
            assertThat(credentialCache.getCurrentCacheSizeBytes())
                    .isEqualTo(credentialCache.getMaximumCacheSizeBytes());

            assertThat(credentialCache.remove(new Credential(username + i, password))).isTrue();
            assertThat(credentialCache.contains(new Credential(username + 1, password))).isFalse();

            assertThat(credentialCache.getCurrentCacheSizeBytes()).isEqualTo(0);
        }

        credential = new Credential(username + 10, password);
        credentialCache.add(credential);
        assertThat(credentialCache.contains(credential)).isFalse();
    }

    @Test
    public void test3() {
        String username = "prometheus";
        String password = "secret";
        Credential credential = new Credential(username + "X", password);
        int credentialSizeBytes = credential.toString().getBytes(StandardCharsets.UTF_8).length;
        int maximumCacheSizeBytes = credentialSizeBytes * 10;

        CredentialCache credentialCache = new CredentialCache(maximumCacheSizeBytes);

        for (int i = 0; i < 10; i++) {
            credential = new Credential(username + i, password);

            credentialCache.add(credential);

            assertThat(credentialCache.contains(credential)).isTrue();
            assertThat(credentialCache.contains(new Credential(username + i, password))).isTrue();
            assertThat(credentialCache.getCurrentCacheSizeBytes())
                    .isEqualTo(credentialSizeBytes * (i + 1));
        }

        assertThat(credentialCache.getCurrentCacheSizeBytes()).isEqualTo(maximumCacheSizeBytes);

        credentialCache.add(new Credential(username + 0, password));

        assertThat(credentialCache.contains(new Credential(username + 0, password))).isTrue();
        assertThat(credentialCache.getCurrentCacheSizeBytes()).isEqualTo(maximumCacheSizeBytes);
    }

    @Test
    public void test4() {
        String username = "prometheus";
        String password = "secret";
        Credential credential = new Credential(username, password);
        int credentialSizeBytes = credential.toString().getBytes(StandardCharsets.UTF_8).length;
        int maximumCacheSizeBytes = credentialSizeBytes;

        CredentialCache credentialCache = new CredentialCache(maximumCacheSizeBytes);

        credentialCache.add(credential);

        assertThat(credentialCache.contains(credential)).isTrue();
        assertThat(credentialCache.getCurrentCacheSizeBytes()).isEqualTo(maximumCacheSizeBytes);

        credential = new Credential(username + "012345678", password);

        credentialCache.add(credential);

        assertThat(credentialCache.contains(credential)).isFalse();
        assertThat(credentialCache.getCurrentCacheSizeBytes()).isEqualTo(credentialSizeBytes);
    }
}
