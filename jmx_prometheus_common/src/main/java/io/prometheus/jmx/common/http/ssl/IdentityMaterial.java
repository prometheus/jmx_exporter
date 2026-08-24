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

package io.prometheus.jmx.common.http.ssl;

import io.prometheus.jmx.common.util.MapAccessor;
import nl.altindag.ssl.SSLFactory;

/**
 * Package-private interface for applying identity material to an SSLFactory builder.
 *
 * <p>Implementations know how to apply their specific identity material (keystore or PEM) and
 * detect file changes for reload.
 */
public interface IdentityMaterial {

    /**
     * Applies this identity material to the SSLFactory builder.
     *
     * <p>Implementations MUST call {@code withSwappableIdentityMaterial()} before
     * {@code withIdentityMaterial(...)} to enable runtime context swapping via {@code
     * SSLFactoryUtils.reload()}.
     *
     * @param builder the SSLFactory builder to configure
     */
    void applyTo(SSLFactory.Builder builder);

    /**
     * Returns true if the underlying identity files have changed since last load.
     *
     * @return true if files changed, false if unchanged or unreadable at runtime
     */
    boolean hasChanged();

    /**
     * Reloads identity material from configuration. Returns a new instance on success, or this
     * same instance on failure (indicating prior identity should be retained).
     *
     * @param rootMapAccessor the root configuration map accessor
     * @return a new IdentityMaterial on success, or this instance on failure
     */
    IdentityMaterial reload(MapAccessor rootMapAccessor);
}
