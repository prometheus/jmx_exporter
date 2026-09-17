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
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for the allocation-light string cache probe added to
 * {@link CredentialsCache}. The string overloads must behave identically to the
 * {@link Credentials}-based API so the authenticators can use them on the cache-hit path without a
 * full {@code Credentials} (and its UTF-8 {@code byte[]} copies).
 */
public class CredentialsCacheProbeTest {

    @Test
    public void stringAndCredentialsOverloadsAreInterchangeable() {
        CredentialsCache cache = new CredentialsCache(CredentialsCache.DEFAULT_MAX_VALUE_SIZE_BYTES, 100);

        cache.add("prometheus", "secret");

        assertThat(cache.contains("prometheus", "secret")).isTrue();
        assertThat(cache.contains(new Credentials("prometheus", "secret"))).isTrue();
        assertThat(cache.contains("prometheus", "wrong")).isFalse();
        assertThat(cache.contains("wrong", "secret")).isFalse();

        assertThat(cache.remove(new Credentials("prometheus", "secret"))).isTrue();
        assertThat(cache.contains("prometheus", "secret")).isFalse();
    }

    @Test
    public void credentialsAddedViaCredentialsAreFoundViaStrings() {
        CredentialsCache cache = new CredentialsCache(CredentialsCache.DEFAULT_MAX_VALUE_SIZE_BYTES, 100);
        Credentials credentials = new Credentials("User", "Password");

        cache.add(credentials);

        assertThat(cache.contains("User", "Password")).isTrue();
    }

    @Test
    public void probeDistinguishesMismatchedLengthsAndSharedPrefixes() {
        CredentialsCache cache = new CredentialsCache(CredentialsCache.DEFAULT_MAX_VALUE_SIZE_BYTES, 100);

        cache.add("administrator", "correct-horse-battery-staple");

        assertThat(cache.contains("administrator", "correct-horse-battery-stapl"))
                .isFalse();
        assertThat(cache.contains("administrator", "correct-horse-battery-staple-extra"))
                .isFalse();
        assertThat(cache.contains("administrato", "correct-horse-battery-staple"))
                .isFalse();
        assertThat(cache.contains("administratorx", "correct-horse-battery-staple"))
                .isFalse();
        assertThat(cache.contains("administrator", "correct-horse-battery-staple"))
                .isTrue();
    }

    @Test
    public void probeHandlesUnicodeCredentials() {
        CredentialsCache cache = new CredentialsCache(CredentialsCache.DEFAULT_MAX_VALUE_SIZE_BYTES, 100);
        String username = "user-\u00e9\u00e8\u00ea";
        String password = "p\u00e4ssw\u00f6rd-\u4f60\u597d";

        cache.add(username, password);

        assertThat(cache.contains(username, password)).isTrue();
        assertThat(cache.contains(new Credentials(username, password))).isTrue();
        assertThat(cache.contains(username, password + "!")).isFalse();
    }

    @Test
    public void probeHandlesEmptyCredentials() {
        CredentialsCache cache = new CredentialsCache(CredentialsCache.DEFAULT_MAX_VALUE_SIZE_BYTES, 100);

        cache.add("", "");

        assertThat(cache.contains("", "")).isTrue();
        assertThat(cache.contains("", "x")).isFalse();
        assertThat(cache.contains("x", "")).isFalse();
    }

    @Test
    public void oversizedCredentialsAddedViaStringsAreNotCached() {
        CredentialsCache cache = new CredentialsCache(8, 10);
        String oversizedUsername = "oversized-username";
        String oversizedPassword = "oversized-password";

        cache.add(oversizedUsername, oversizedPassword);

        assertThat(cache.contains(oversizedUsername, oversizedPassword)).isFalse();
        assertThat(cache.getCurrentEntries()).isZero();
    }

    @Test
    public void stringProbeStillHonorsWeightBound() {
        // "user-x" + "password-x" is 16 UTF-8 bytes, so a 32-byte weight limit holds two entries.
        CredentialsCache cache = new CredentialsCache(16, 2);

        cache.add("user-a", "password-a");
        cache.add("user-b", "password-b");
        cache.add("user-c", "password-c");

        assertThat(cache.getCurrentEntries()).isLessThanOrEqualTo(2);
    }

    @Test
    public void stringOverloadsRejectNulls() {
        CredentialsCache cache = new CredentialsCache(CredentialsCache.DEFAULT_MAX_VALUE_SIZE_BYTES, 10);

        assertThatThrownBy(() -> cache.add(null, "password")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.add("username", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.contains(null, "password")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.contains("username", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
