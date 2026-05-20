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
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class CredentialsCacheTest {

    @Test
    public void basicAddContainsAndRemoveTest() {
        Credentials credentials = new Credentials("prometheus", "secret");
        int credentialSizeBytes = credentials.byteSize();

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

        CredentialsCache credentialsCache = new CredentialsCache(cachedCredentials.byteSize(), 2);
        credentialsCache.add(cachedCredentials);
        credentialsCache.add(oversizedCredentials);

        assertThat(credentialsCache.contains(cachedCredentials)).isTrue();
        assertThat(credentialsCache.contains(oversizedCredentials)).isFalse();
        assertThat(credentialsCache.getCurrentEntries()).isEqualTo(1);
    }

    @Test
    public void cacheRemainsBoundedByWeight() {
        Credentials credentialsA = new Credentials("user-a", "password-a");
        Credentials credentialsB = new Credentials("user-b", "password-b");
        Credentials credentialsC = new Credentials("user-c", "password-c");

        CredentialsCache credentialsCache = new CredentialsCache(credentialsA.byteSize(), 2);
        credentialsCache.add(credentialsA);
        credentialsCache.add(credentialsB);
        credentialsCache.add(credentialsC);

        assertThat(credentialsCache.getCurrentEntries()).isLessThanOrEqualTo(2);
    }

    @Test
    public void containsAndAddStayWithinWeightBound() {
        Credentials credentialsA = new Credentials("user-a", "password-a");
        Credentials credentialsB = new Credentials("user-b", "password-b");
        Credentials credentialsC = new Credentials("user-c", "password-c");
        Credentials credentialsD = new Credentials("user-d", "password-d");

        CredentialsCache credentialsCache = new CredentialsCache(credentialsA.byteSize(), 3);
        credentialsCache.add(credentialsA);
        credentialsCache.add(credentialsB);
        credentialsCache.add(credentialsC);

        credentialsCache.contains(credentialsA);
        credentialsCache.contains(credentialsB);
        credentialsCache.contains(credentialsC);

        credentialsCache.add(credentialsD);

        assertThat(credentialsCache.getCurrentEntries()).isLessThanOrEqualTo(3);
    }

    @Test
    public void reAddingExistingEntryDoesNotIncreaseSize() {
        Credentials credentialsA = new Credentials("user-a", "password-a");
        Credentials credentialsB = new Credentials("user-b", "password-b");

        CredentialsCache credentialsCache = new CredentialsCache(credentialsA.byteSize(), 10);
        credentialsCache.add(credentialsA);
        credentialsCache.add(credentialsB);
        credentialsCache.add(credentialsA);

        assertThat(credentialsCache.getCurrentEntries()).isEqualTo(2);
        assertThat(credentialsCache.contains(credentialsA)).isTrue();
        assertThat(credentialsCache.contains(credentialsB)).isTrue();
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

    @Test
    public void constructorRejectsNonPositiveWeightLimits() {
        assertThatThrownBy(() -> new CredentialsCache(1, 0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CredentialsCache(1, -1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void cacheEvictsWhenWeightExceeded() {
        Credentials credentialsA = new Credentials("user-a", "password-a");
        Credentials credentialsB = new Credentials("user-b", "password-b");
        Credentials credentialsC = new Credentials("user-c", "password-c");

        CredentialsCache credentialsCache = new CredentialsCache(credentialsA.byteSize(), 2);
        credentialsCache.add(credentialsA);
        credentialsCache.add(credentialsB);

        assertThat(credentialsCache.getCurrentEntries()).isEqualTo(2);

        credentialsCache.add(credentialsC);

        assertThat(credentialsCache.getCurrentEntries()).isLessThanOrEqualTo(2);
    }

    @Test
    public void getReturnsNullForAbsentCredentials() {
        CredentialsCache credentialsCache = new CredentialsCache(16, 1);

        assertThat(credentialsCache.get(new Credentials("missing", "entry"))).isNull();
    }

    @Test
    public void getReturnsTrueForValidCredentials() {
        Credentials credentials = new Credentials("user", "pass");
        CredentialsCache credentialsCache = new CredentialsCache(credentials.byteSize(), 1);

        credentialsCache.add(credentials);

        assertThat(credentialsCache.get(credentials)).isTrue();
        assertThat(credentialsCache.contains(credentials)).isTrue();
    }

    @Test
    public void getReturnsFalseForInvalidCredentials() {
        Credentials credentials = new Credentials("user", "pass");
        CredentialsCache credentialsCache = new CredentialsCache(credentials.byteSize(), 1);

        credentialsCache.addInvalid(credentials);

        assertThat(credentialsCache.get(credentials)).isFalse();
        assertThat(credentialsCache.contains(credentials)).isTrue();
    }

    @Test
    public void addInvalidOverwritesPreviousEntry() {
        Credentials credentials = new Credentials("user", "pass");
        CredentialsCache credentialsCache = new CredentialsCache(credentials.byteSize(), 1);

        credentialsCache.add(credentials);
        assertThat(credentialsCache.get(credentials)).isTrue();

        credentialsCache.addInvalid(credentials);
        assertThat(credentialsCache.get(credentials)).isFalse();
    }

    @Test
    public void ttlConstructorEvictsAfterDuration() throws Exception {
        Credentials credentials = new Credentials("user", "pass");
        CredentialsCache credentialsCache = new CredentialsCache(credentials.byteSize(), 10, Duration.ofMillis(10));

        credentialsCache.add(credentials);
        assertThat(credentialsCache.contains(credentials)).isTrue();

        Thread.sleep(100);

        assertThat(credentialsCache.contains(credentials)).isFalse();
    }

    @Test
    public void negativeTtlIsTreatedAsNoExpiry() {
        Credentials credentials = new Credentials("user", "pass");
        CredentialsCache credentialsCache = new CredentialsCache(credentials.byteSize(), 10, Duration.ofMillis(-1));

        credentialsCache.add(credentials);
        assertThat(credentialsCache.contains(credentials)).isTrue();
        assertThat(credentialsCache.get(credentials)).isTrue();
    }

    @Test
    public void getMaxWeightBytesReturnsConfiguredValue() {
        CredentialsCache credentialsCache = new CredentialsCache(16, 100);
        assertThat(credentialsCache.getMaxWeightBytes()).isEqualTo(1600L);
    }

    @Test
    public void oversizedCredentialsNotCachedForAddInvalid() {
        Credentials cachedCredentials = new Credentials("ab", "cd");
        Credentials oversizedCredentials = new Credentials("oversized-username", "oversized-password");

        CredentialsCache credentialsCache = new CredentialsCache(cachedCredentials.byteSize(), 2);
        credentialsCache.add(cachedCredentials);
        credentialsCache.addInvalid(oversizedCredentials);

        assertThat(credentialsCache.get(oversizedCredentials)).isNull();
    }

    @Test
    public void constructorWithLongMaxWeightBytes() {
        CredentialsCache credentialsCache = new CredentialsCache(16, 50000L);
        assertThat(credentialsCache.getMaxWeightBytes()).isEqualTo(50000L);
        assertThat(credentialsCache.getMaxEntries()).isEqualTo(3125);
    }

    @Test
    public void constructorWithTtlAndLongWeight() {
        CredentialsCache credentialsCache = new CredentialsCache(16, 50000L, Duration.ofSeconds(60));
        assertThat(credentialsCache.getMaxWeightBytes()).isEqualTo(50000L);
    }
}
