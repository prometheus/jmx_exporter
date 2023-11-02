/*
 * Copyright (C) 2022-2023 The Prometheus jmx_exporter Authors
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
package io.prometheus.jmx;

import java.util.Map;
import java.util.TreeMap;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface ExistDbMXBean {
    Map<QueryKey, RunningQuery> getRunningQueries();

    class QueryKey implements Comparable<QueryKey> {
        private final int id;
        private final String path;

        public QueryKey(final int id, final String path) {
            this.id = id;
            this.path = path;
        }

        public int getId() {
            return id;
        }

        public String getPath() {
            return path;
        }

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

        public int hashCode() {
            int result = id;
            result = 31 * result + path.hashCode();
            return result;
        }

        public int compareTo(final QueryKey other) {
            if (other == null) {
                return 1;
            }

            return path.compareTo(other.path);
        }
    }

    class RunningQuery {
        private final int id;
        private final String path;

        private final long startedAtTime;

        public RunningQuery(final int id, final String path, final long startedAtTime) {
            this.id = id;
            this.path = path;
            this.startedAtTime = startedAtTime;
        }

        public int getId() {
            return id;
        }

        public String getPath() {
            return path;
        }

        public long getStartedAtTime() {
            return startedAtTime;
        }

        public long getElapsedTime() {
            return System.currentTimeMillis() - startedAtTime;
        }
    }
}

class ExistDb implements ExistDbMXBean {

    public static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mxbeanName = new ObjectName("org.exist.management.exist:type=ProcessReport");
        ExistDb mxbean = new ExistDb();
        mbs.registerMBean(mxbean, mxbeanName);
    }

    public Map<QueryKey, RunningQuery> getRunningQueries() {
        final Map<QueryKey, RunningQuery> queries = new TreeMap<>();

        final RunningQuery runningQuery1 =
                new RunningQuery(1, "/db/query1.xq", System.currentTimeMillis());
        final RunningQuery runningQuery2 =
                new RunningQuery(2, "/db/query2.xq", System.currentTimeMillis());

        queries.put(new QueryKey(runningQuery1.getId(), runningQuery1.getPath()), runningQuery1);
        queries.put(new QueryKey(runningQuery2.getId(), runningQuery2.getPath()), runningQuery2);

        return queries;
    }
}
