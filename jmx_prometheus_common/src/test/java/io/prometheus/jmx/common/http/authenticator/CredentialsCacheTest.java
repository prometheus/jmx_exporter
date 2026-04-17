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

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.common.authenticator.Credentials;
import io.prometheus.jmx.common.authenticator.CredentialsCache;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class CredentialsCacheTest {

    @Test
    public void basicTest() {
        String username = "prometheus";
        String password = "secret";
        Credentials credentials = new Credentials(username, password);
        int credentialSizeBytes = credentials.toString().getBytes(StandardCharsets.UTF_8).length;

        CredentialsCache credentialsCache = new CredentialsCache(credentialSizeBytes);

        credentialsCache.add(credentials);

        assertThat(credentialsCache.contains(credentials)).isTrue();
        assertThat(credentialsCache.contains(new Credentials(username, password)))
                .isTrue();
        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(credentialSizeBytes);
        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(credentialsCache.getMaximumCacheSizeBytes());

        assertThat(credentialsCache.remove(new Credentials(username, password))).isTrue();
        assertThat(credentialsCache.contains(new Credentials(username, password)))
                .isFalse();
    }

    @Test
    public void basicTestWithMultipleCredentials() {
        String username = "prometheus";
        String password = "secret";
        Credentials credentials = new Credentials(username + "X", password);
        int credentialSizeBytes = credentials.toString().getBytes(StandardCharsets.UTF_8).length;

        CredentialsCache credentialsCache = new CredentialsCache(credentialSizeBytes);

        for (int i = 0; i < 10; i++) {
            credentials = new Credentials(username + i, password);

            credentialsCache.add(credentials);

            assertThat(credentialsCache.contains(credentials)).isTrue();
            assertThat(credentialsCache.contains(new Credentials(username + i, password)))
                    .isTrue();
            assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(credentialSizeBytes);
            assertThat(credentialsCache.getCurrentCacheSizeBytes())
                    .isEqualTo(credentialsCache.getMaximumCacheSizeBytes());

            assertThat(credentialsCache.remove(new Credentials(username + i, password)))
                    .isTrue();
            assertThat(credentialsCache.contains(new Credentials(username + 1, password)))
                    .isFalse();

            assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(0);
        }

        credentials = new Credentials(username + 10, password);
        credentialsCache.add(credentials);
        assertThat(credentialsCache.contains(credentials)).isFalse();
    }

    @Test
    public void cacheUpdateTest() {
        String username = "prometheus";
        String password = "secret";
        Credentials credentials = new Credentials(username + "X", password);
        int credentialSizeBytes = credentials.toString().getBytes(StandardCharsets.UTF_8).length;
        int maximumCacheSizeBytes = credentialSizeBytes * 10;

        CredentialsCache credentialsCache = new CredentialsCache(maximumCacheSizeBytes);

        for (int i = 0; i < 10; i++) {
            credentials = new Credentials(username + i, password);

            credentialsCache.add(credentials);

            assertThat(credentialsCache.contains(credentials)).isTrue();
            assertThat(credentialsCache.contains(new Credentials(username + i, password)))
                    .isTrue();
            assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(credentialSizeBytes * (i + 1));
        }

        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(maximumCacheSizeBytes);

        credentialsCache.add(new Credentials(username + 0, password));

        assertThat(credentialsCache.contains(new Credentials(username + 0, password)))
                .isTrue();
        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(maximumCacheSizeBytes);
    }

    @Test
    public void cacheOverflowTest() {
        String username = "prometheus";
        String password = "secret";
        Credentials credentials = new Credentials(username, password);
        int credentialSizeBytes = credentials.toString().getBytes(StandardCharsets.UTF_8).length;

        CredentialsCache credentialsCache = new CredentialsCache(credentialSizeBytes);

        credentialsCache.add(credentials);

        assertThat(credentialsCache.contains(credentials)).isTrue();
        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(credentialSizeBytes);

        credentials = new Credentials(username + "012345678", password);

        credentialsCache.add(credentials);

        assertThat(credentialsCache.contains(credentials)).isFalse();
        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(credentialSizeBytes);
    }

    /**
     * Test that verifies correct size accounting during LRU eviction with varying credential sizes.
     * This catches the bug where eviction subtracted the new entry size instead of the
     * evicted entry size, causing incorrect memory accounting.
     */
    @Test
    public void evictionSizeAccountingWithVaryingSizes() {
        Credentials smallCred = new Credentials("ab", "cd");
        int smallSize = smallCred.toString().getBytes(StandardCharsets.UTF_8).length;

        Credentials largeCred = new Credentials("abcdefghijklmnop", "qrstuvwx");
        int largeSize = largeCred.toString().getBytes(StandardCharsets.UTF_8).length;

        int maxCacheSize = smallSize * 2 + largeSize;
        CredentialsCache credentialsCache = new CredentialsCache(maxCacheSize);

        credentialsCache.add(smallCred);
        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(smallSize);

        credentialsCache.add(largeCred);
        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(smallSize + largeSize);

        Credentials slightlyLargerCred = new Credentials("ab", "cd!");
        int slightlyLargerSize = slightlyLargerCred.toString().getBytes(StandardCharsets.UTF_8).length;
        credentialsCache.add(slightlyLargerCred);
        // Adding slightlyLargerCred evicts smallCred (LRU) since maxCacheSize = smallSize * 2 + largeSize
        // After eviction: largeCred(24) + slightlyLargerCred(5) = 29
        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isEqualTo(largeSize + slightlyLargerSize);

        credentialsCache.add(new Credentials("zz", "zz"));
        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isGreaterThan(0);
        assertThat(credentialsCache.getCurrentCacheSizeBytes()).isLessThanOrEqualTo(maxCacheSize);
    }
}
