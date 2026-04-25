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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.prometheus.jmx.common.authenticator.Credentials;
import io.prometheus.jmx.common.authenticator.CredentialsCache;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class CredentialsCacheTest {

    @Test
    public void basicAddContainsAndRemoveTest() {
        Credentials credentials = new Credentials("prometheus", "secret");
        int credentialSizeBytes = sizeBytes(credentials);

        CredentialsCache credentialsCache = new CredentialsCache(credentialSizeBytes, 1);

        credentialsCache.add(credentials);

        assertThat(credentialsCache.contains(credentials)).isTrue();
        assertThat(credentialsCache.contains(new Credentials("prometheus", "secret")))
                .isTrue();
        assertThat(credentialsCache.getCurrentEntries()).isEqualTo(1);
        assertThat(credentialsCache.getMaxValueSizeBytes()).isEqualTo(credentialSizeBytes);
        assertThat(credentialsCache.getMaxEntries()).isEqualTo(1);

        assertThat(credentialsCache.remove(new Credentials("prometheus", "secret")))
                .isTrue();
        assertThat(credentialsCache.contains(new Credentials("prometheus", "secret")))
                .isFalse();
        assertThat(credentialsCache.getCurrentEntries()).isZero();
    }

    @Test
    public void oversizedCredentialsAreNotCachedAndDoNotEvictExistingEntries() {
        Credentials cachedCredentials = new Credentials("ab", "cd");
        Credentials oversizedCredentials = new Credentials("oversized-username", "oversized-password");

        CredentialsCache credentialsCache = new CredentialsCache(sizeBytes(cachedCredentials), 2);
        credentialsCache.add(cachedCredentials);
        credentialsCache.add(oversizedCredentials);

        assertThat(credentialsCache.contains(cachedCredentials)).isTrue();
        assertThat(credentialsCache.contains(oversizedCredentials)).isFalse();
        assertThat(credentialsCache.getCurrentEntries()).isEqualTo(1);
    }

    @Test
    public void addEvictsLeastRecentlyUsedEntryWhenMaxEntriesExceeded() {
        Credentials credentialsA = new Credentials("user-a", "password-a");
        Credentials credentialsB = new Credentials("user-b", "password-b");
        Credentials credentialsC = new Credentials("user-c", "password-c");

        CredentialsCache credentialsCache = new CredentialsCache(sizeBytes(credentialsA), 2);
        credentialsCache.add(credentialsA);
        credentialsCache.add(credentialsB);
        credentialsCache.add(credentialsC);

        assertThat(credentialsCache.contains(credentialsA)).isFalse();
        assertThat(credentialsCache.contains(credentialsB)).isTrue();
        assertThat(credentialsCache.contains(credentialsC)).isTrue();
        assertThat(credentialsCache.getCurrentEntries()).isEqualTo(2);
    }

    @Test
    public void containsRefreshesRecencyBeforeEviction() {
        Credentials credentialsA = new Credentials("user-a", "password-a");
        Credentials credentialsB = new Credentials("user-b", "password-b");
        Credentials credentialsC = new Credentials("user-c", "password-c");
        Credentials credentialsD = new Credentials("user-d", "password-d");

        CredentialsCache credentialsCache = new CredentialsCache(sizeBytes(credentialsA), 3);
        credentialsCache.add(credentialsA);
        credentialsCache.add(credentialsB);
        credentialsCache.add(credentialsC);

        assertThat(credentialsCache.contains(credentialsA)).isTrue();

        credentialsCache.add(credentialsD);

        assertThat(credentialsCache.contains(credentialsA)).isTrue();
        assertThat(credentialsCache.contains(credentialsB)).isFalse();
        assertThat(credentialsCache.contains(credentialsC)).isTrue();
        assertThat(credentialsCache.contains(credentialsD)).isTrue();
    }

    @Test
    public void reAddingExistingEntryRefreshesRecencyWithoutCreatingDuplicate() {
        Credentials credentialsA = new Credentials("user-a", "password-a");
        Credentials credentialsB = new Credentials("user-b", "password-b");
        Credentials credentialsC = new Credentials("user-c", "password-c");

        CredentialsCache credentialsCache = new CredentialsCache(sizeBytes(credentialsA), 2);
        credentialsCache.add(credentialsA);
        credentialsCache.add(credentialsB);

        credentialsCache.add(credentialsA);
        credentialsCache.add(credentialsC);

        assertThat(credentialsCache.contains(credentialsA)).isTrue();
        assertThat(credentialsCache.contains(credentialsB)).isFalse();
        assertThat(credentialsCache.contains(credentialsC)).isTrue();
        assertThat(credentialsCache.getCurrentEntries()).isEqualTo(2);
    }

    @Test
    public void removeReturnsFalseWhenEntryIsMissing() {
        CredentialsCache credentialsCache = new CredentialsCache(16, 1);

        assertThat(credentialsCache.remove(new Credentials("missing", "entry"))).isFalse();
    }

    @Test
    public void constructorRejectsNonPositiveLimits() {
        assertThatThrownBy(() -> new CredentialsCache(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CredentialsCache(1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static int sizeBytes(Credentials credentials) {
        return credentials.toString().getBytes(StandardCharsets.UTF_8).length;
    }
}
