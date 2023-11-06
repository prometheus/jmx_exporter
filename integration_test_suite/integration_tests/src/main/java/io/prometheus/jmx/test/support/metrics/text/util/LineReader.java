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

package io.prometheus.jmx.test.support.metrics.text.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;

public class LineReader implements AutoCloseable {

    private final LinkedList<String> lineBuffer;
    private BufferedReader bufferedReader;

    public LineReader(String string) {
        this.bufferedReader = new BufferedReader(new StringReader(string));
        this.lineBuffer = new LinkedList<>();
    }

    public String readLine() throws IOException {
        if (!lineBuffer.isEmpty()) {
            return lineBuffer.removeLast();
        } else {
            return bufferedReader.readLine();
        }
    }

    public void unreadLine(String line) {
        lineBuffer.add(line);
    }

    @Override
    public void close() {
        lineBuffer.clear();

        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (Throwable t) {
                // DO NOTHING
            }

            bufferedReader = null;
        }
    }
}
