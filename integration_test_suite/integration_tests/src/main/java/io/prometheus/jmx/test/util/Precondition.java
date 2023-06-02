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

package io.prometheus.jmx.test.util;

import java.io.File;

public class Precondition {

    private Precondition() {
        // DO NOTHING
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static String notEmpty(String string, String message) {
        string = string.trim();

        if (string.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return string;
    }

    public static void inRange(short value, short min, short max, String message) {
        if ((value < min) || (value > max)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void inRange(int value, int min, int max, String message) {
        if ((value < min) || (value > max)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void inRange(long value, long min, long max, String message) {
        if ((value < min) || (value > max)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isTrue(boolean bool, String message) {
        if (!bool) {
            throw new IllegalStateException(message);
        }
    }

    public static void exists(File file, String message) {
        if (!file.exists()) {
            throw new IllegalStateException(message);
        }
    }

    public static void isFile(File file, String message) {
        if (!file.isFile()) {
            throw new IllegalStateException(message);
        }
    }

    public static void canRead(File file, String message) {
        if (!file.canRead()) {
            throw new IllegalStateException(message);
        }
    }
}
