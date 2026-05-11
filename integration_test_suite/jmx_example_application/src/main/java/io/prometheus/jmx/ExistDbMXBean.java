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

/**
 * MXBean interface exposing running query information for a simulated eXist-db instance.
 *
 * <p>The {@link #getRunningQueries()} method returns a map keyed by {@link QueryKey}, enabling
 * integration testing of complex composite and tabular JMX attribute types.
 */
public interface ExistDbMXBean {

    /**
     * Returns the map of currently running queries keyed by query identity.
     *
     * @return a map of running queries, keyed by {@link QueryKey}; never {@code null}
     */
    Map<QueryKey, RunningQuery> getRunningQueries();

    /**
     * Composite key identifying a running query by its numeric ID and path.
     *
     * <p>Natural ordering is by path, as required by {@link Comparable}.
     */
    class QueryKey implements Comparable<QueryKey> {

        private final int id;
        private final String path;

        /**
         * Constructs a new query key.
         *
         * @param id the numeric query identifier
         * @param path the query path, such as {@code /db/query1.xq}
         */
        public QueryKey(final int id, final String path) {
            this.id = id;
            this.path = path;
        }

        /**
         * Returns the numeric query identifier.
         *
         * @return the query ID
         */
        public int getId() {
            return id;
        }

        /**
         * Returns the query path.
         *
         * @return the path of the query
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

    /**
     * Represents a currently executing query with an ID, path, and start time.
     */
    class RunningQuery {

        private final int id;
        private final String path;

        private final long startedAtTime;

        /**
         * Constructs a new running query.
         *
         * @param id the numeric query identifier
         * @param path the query path, such as {@code /db/query1.xq}
         * @param startedAtTime the epoch millisecond timestamp when the query started
         */
        public RunningQuery(final int id, final String path, final long startedAtTime) {
            this.id = id;
            this.path = path;
            this.startedAtTime = startedAtTime;
        }

        /**
         * Returns the numeric query identifier.
         *
         * @return the query ID
         */
        public int getId() {
            return id;
        }

        /**
         * Returns the query path.
         *
         * @return the path of the query
         */
        public String getPath() {
            return path;
        }

        /**
         * Returns the epoch millisecond timestamp when the query started.
         *
         * @return the start time in epoch milliseconds
         */
        public long getStartedAtTime() {
            return startedAtTime;
        }

        /**
         * Returns the number of milliseconds elapsed since the query started.
         *
         * @return the elapsed time in milliseconds
         */
        public long getElapsedTime() {
            return System.currentTimeMillis() - startedAtTime;
        }
    }
}
