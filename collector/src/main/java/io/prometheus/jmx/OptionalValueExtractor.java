/*
 * Copyright (C) 2021-2023 The Prometheus jmx_exporter Authors
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OptionalValueExtractor {

    private static final Logger LOGGER = Logger.getLogger(OptionalValueExtractor.class.getName());

    private static final String OPTIONAL_CLASS_NAME = "java.util.Optional";
    private static final Class<?> OPTIONAL_CLASS = findOptionalClass();
    private static final Method OR_ELSE_METHOD = findOrElseMethod();

    private static Class<?> findOptionalClass() {
        Class<?> optionalClass = null;
        try {
            optionalClass = Class.forName(OPTIONAL_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.FINE, "{0}: class not found (not running on Java 8+)", OPTIONAL_CLASS_NAME); // that's okay
        }
        if (optionalClass != null) {
            LOGGER.log(Level.FINE, "{0} will be supported", OPTIONAL_CLASS_NAME);
        }
        return optionalClass;
    }

    private static Method findOrElseMethod() {
        if (OPTIONAL_CLASS != null) {
            try {
                return OPTIONAL_CLASS.getMethod("orElse", Object.class);
            } catch (NoSuchMethodException e) {
                LOGGER.log(Level.WARNING, "{0}.orElse(Object): method not found!", OPTIONAL_CLASS_NAME); // that would be weird!
            }
        }
        return null;
    }

    public boolean isOptional(Object o) {
        return (o != null)
                && OPTIONAL_CLASS == o.getClass() // Optional is final, no need for isAssignableFrom()
                && OR_ELSE_METHOD != null; // would we be able to extract value from this Optional?
    }

    public Object getOptionalValueOrNull(Object o) {
        try {
            return OR_ELSE_METHOD.invoke(o, (Object) null);
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.FINE, "IllegalAccessException calling orElse(null) on {0}", o);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.FINE, "IllegalArgumentException calling orElse(null) on {0}", o);
        } catch (InvocationTargetException e) {
            LOGGER.log(Level.FINE, "InvocationTargetException calling orElse(null) on {0}", o);
        }
        return null;
    }
}
