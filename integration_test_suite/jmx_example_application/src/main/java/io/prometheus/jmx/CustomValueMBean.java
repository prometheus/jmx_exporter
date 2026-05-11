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

package io.prometheus.jmx;

/**
 * MBean interface exposing a fixed integer value and a fixed text string.
 */
public interface CustomValueMBean {

    /**
     * Returns the fixed integer value.
     *
     * @return the integer value exposed by this MBean
     */
    Integer getValue();

    /**
     * Returns the fixed text string.
     *
     * @return the text string exposed by this MBean
     */
    String getText();
}
