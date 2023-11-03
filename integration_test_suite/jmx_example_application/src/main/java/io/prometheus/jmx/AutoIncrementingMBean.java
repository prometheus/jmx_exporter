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

import java.util.concurrent.atomic.AtomicInteger;

public interface AutoIncrementingMBean {

    int getValue();
}

class AutoIncrementing implements AutoIncrementingMBean {

    private final AtomicInteger atomicInteger;

    public AutoIncrementing() {
        atomicInteger = new AtomicInteger(0);
    }

    @Override
    public int getValue() {
        return atomicInteger.getAndIncrement();
    }
}
