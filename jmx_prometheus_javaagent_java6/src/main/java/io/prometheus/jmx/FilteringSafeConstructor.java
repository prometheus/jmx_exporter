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

package io.prometheus.jmx;

import org.yaml.snakeyaml.constructor.Constructor;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FilteringSafeConstructor extends Constructor {

    private Set<String> allowedClassNameSet;

    public FilteringSafeConstructor() {
        super();

        allowedClassNameSet = new HashSet<String>();
        allowedClassNameSet.add(null);
        allowedClassNameSet.add(BigInteger.class.getName());
        allowedClassNameSet.add(Boolean.class.getName());
        allowedClassNameSet.add(Byte.class.getName());
        allowedClassNameSet.add(Character.class.getName());
        allowedClassNameSet.add(Date.class.getName());
        allowedClassNameSet.add(java.sql.Date.class.getName());
        allowedClassNameSet.add(Double.class.getName());
        allowedClassNameSet.add(Float.class.getName());
        allowedClassNameSet.add(Integer.class.getName());
        allowedClassNameSet.add(List.class.getName());
        allowedClassNameSet.add(Long.class.getName());
        allowedClassNameSet.add(Map.class.getName());
        allowedClassNameSet.add(Set.class.getName());
        allowedClassNameSet.add(Short.class.getName());
        allowedClassNameSet.add(String.class.getName());
        allowedClassNameSet.add(Timestamp.class.getName());
    }

    @Override
    protected Class<?> getClassForName(String name) throws ClassNotFoundException {
        if (!allowedClassNameSet.contains(name)) {
            throw new IllegalStateException(String.format("Class [%s] isn't allowed", name));
        }

        return super.getClassForName(name);
    }
}
