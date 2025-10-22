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

import java.util.Date;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface CamelMBean {
    double EXPECTED_SECONDS = 1.573285945111E9;

    Date getLastExchangeFailureTimestamp();
}

class Camel implements CamelMBean {
    public static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mbeanName =
                new ObjectName(
                        "org.apache.camel:context=my-camel-context,type=routes,name=\"my-route-name\"");
        Camel mbean = new Camel();
        mbs.registerMBean(mbean, mbeanName);
    }

    @Override
    public Date getLastExchangeFailureTimestamp() {
        return new Date((long) (EXPECTED_SECONDS * 1000));
    }
}
