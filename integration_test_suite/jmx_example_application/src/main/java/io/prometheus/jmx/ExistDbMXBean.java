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

import java.util.Map;

/** Interface to implement ExistDbMXBean */
public interface ExistDbMXBean {

    /**
     * Method to get the Map of running queries
     *
     * @return a Map of running queries
     */
    Map<QueryKey, RunningQuery> getRunningQueries();

    /** Class to implement QueryKey */
    class QueryKey implements Comparable<QueryKey> {

        private final int id;
        private final String path;

        /**
         * Constructor
         *
         * @param id id
         * @param path path
         */
        public QueryKey(final int id, final String path) {
            this.id = id;
            this.path = path;
        }

        /**
         * Method to get the id
         *
         * @return the id
         */
        public int getId() {
            return id;
        }

        /**
         * Method to get the path
         *
         * @return the path
         */
        public String getPath() {
            return path;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            QueryKey queryKey = (QueryKey) other;
            if (id != queryKey.id) {
                return false;
            }
            return path.equals(queryKey.path);
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + path.hashCode();
            return result;
        }

        @Override
        public int compareTo(final QueryKey other) {
            if (other == null) {
                return 1;
            }

            return path.compareTo(other.path);
        }
    }

    /** Class to implement RunningQuery */
    class RunningQuery {

        private final int id;
        private final String path;

        private final long startedAtTime;

        /**
         * Constructor
         *
         * @param id id
         * @param path path
         * @param startedAtTime startedAtTime
         */
        public RunningQuery(final int id, final String path, final long startedAtTime) {
            this.id = id;
            this.path = path;
            this.startedAtTime = startedAtTime;
        }

        /**
         * Method to get the id
         *
         * @return the id
         */
        public int getId() {
            return id;
        }

        /**
         * Method to get the path
         *
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /**
         * Method to get the start time
         *
         * @return the start time
         */
        public long getStartedAtTime() {
            return startedAtTime;
        }

        /**
         * Method to get the elapsed time
         *
         * @return the elapsed time
         */
        public long getElapsedTime() {
            return System.currentTimeMillis() - startedAtTime;
        }
    }
}
