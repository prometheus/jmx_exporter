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

package io.prometheus.jmx.test.support;

import java.util.Objects;

/** Class to implement a Label */
public class Label {

    private final String name;
    private final String value;

    /**
     * Constructor
     *
     * @param name name
     * @param value value
     */
    private Label(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Method to get the name
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Method to get the value
     *
     * @return the value
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label = (Label) o;
        return Objects.equals(name, label.name) && Objects.equals(value, label.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    /**
     * Method to create a Label
     *
     * @param name name
     * @param value value
     * @return a Label
     */
    public static Label of(String name, String value) {
        return new Label(name, value);
    }
}
