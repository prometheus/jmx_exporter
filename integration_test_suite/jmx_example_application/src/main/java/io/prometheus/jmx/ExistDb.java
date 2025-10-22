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

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.TreeMap;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/** Class to implement ExitDb */
public class ExistDb implements ExistDbMXBean {

    private final Map<QueryKey, RunningQuery> queries;

    public ExistDb() {
        queries = build();
    }

    @Override
    public Map<QueryKey, RunningQuery> getRunningQueries() {
        return queries;
    }

    private Map<QueryKey, RunningQuery> build() {
        Map<QueryKey, RunningQuery> queries = new TreeMap<>();

        RunningQuery runningQuery1 =
                new RunningQuery(1, "/db/query1.xq", System.currentTimeMillis());

        RunningQuery runningQuery2 =
                new RunningQuery(2, "/db/query2.xq", System.currentTimeMillis());

        queries.put(new QueryKey(runningQuery1.getId(), runningQuery1.getPath()), runningQuery1);
        queries.put(new QueryKey(runningQuery2.getId(), runningQuery2.getPath()), runningQuery2);

        return queries;
    }

    /**
     * Method to register the MBean
     *
     * @throws Exception If an error occurs during registration
     */
    public void register() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(
                new ExistDb(), new ObjectName("org.exist.management.exist:type=ProcessReport"));
    }
}
