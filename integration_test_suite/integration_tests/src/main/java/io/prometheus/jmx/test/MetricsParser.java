/*
 * Copyright 2022-2023 Douglas Hoard
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

package io.prometheus.jmx.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to implements a Metrics response parser
 */
public final class MetricsParser {

    /**
     * Constructor
     */
    private MetricsParser() {
        // DO NOTHING
    }

    /**
     * Method to parse a response as a list of Metric objects
     * <p/>
     * A List is used because Metrics could have the same name, but with different labels
     *
     * @param content
     * @return
     * @throws IOException
     */
    public static List<Metric> parse(String content) throws IOException {
        List<Metric> metricList = new ArrayList<>();

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new StringReader(content));
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    metricList.add(new Metric(line));
                }
            }
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Throwable t) {
                    // DO NOTHING
                }
            }
        }
        return metricList;
    }
}
