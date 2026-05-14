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

import java.util.Optional;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface OptionalValueMBean {

    Optional<Integer> getOptionalPresent();

    Optional<Integer> getOptionalEmpty();
}

class OptionalValue implements OptionalValueMBean {

    @Override
    public Optional<Integer> getOptionalPresent() {
        return Optional.of(42);
    }

    @Override
    public Optional<Integer> getOptionalEmpty() {
        return Optional.empty();
    }

    public static void registerBean(MBeanServer mbs) throws JMException {
        ObjectName mbeanName = new ObjectName("io.prometheus.jmx.test:type=optionalValue");
        OptionalValueMBean mbean = new OptionalValue();
        mbs.registerMBean(mbean, mbeanName);
    }
}
