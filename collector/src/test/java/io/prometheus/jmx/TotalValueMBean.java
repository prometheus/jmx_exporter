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

import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface TotalValueMBean {

    int getTotal();
}

class TotalValue implements TotalValueMBean {

    public TotalValue() {
        // INTENTIONALLY BLANK
    }

    @Override
    public int getTotal() {
        return 345;
    }

    static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mxbeanName = new ObjectName("io.prometheus.jmx.test:type=Total-Value");
        TotalValue mxbean = new TotalValue();
        mbs.registerMBean(mxbean, mxbeanName);

        mxbeanName = new ObjectName("io.prometheus.jmx.test:type=Total.Value");
        mxbean = new TotalValue();
        mbs.registerMBean(mxbean, mxbeanName);
    }
}
