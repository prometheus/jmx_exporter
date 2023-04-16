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

package io.prometheus.jmx.test;

import io.prometheus.jmx.test.util.Precondition;

import java.util.regex.Pattern;

public class Resource {

    private String resourcePath;

    /**
     * Constructor
     *
     * @param resourcePath
     */
    public Resource(String resourcePath) {
        Precondition.notNull(resourcePath, "resource is null");
        Precondition.notEmpty(resourcePath, "resource is empty");

        this.resourcePath = resourcePath.trim();
    }

    /**
     * Method to get the resource path
     * @return
     */
    public String path() {
        return resourcePath;
    }

    public String name() {
        String name = resourcePath;
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        return name;
    }

    @Override
    public String toString() {
        return path();
    }

    public static Resource of(String resourcePath) {
        return new Resource(resourcePath);
    }

    public static Resource of(Class<?> clazz, String resourcePath) {
        Precondition.notNull(clazz, "clazz is null");
        Precondition.notNull(resourcePath, "resource is null");
        Precondition.notEmpty(resourcePath, "resource is empty");

        String classResourcePath = clazz.getName().replaceAll(Pattern.quote("."), "/");
        if (!resourcePath.startsWith("/")) {
            resourcePath = "/" + resourcePath;
        }

        return new Resource("/" + classResourcePath + resourcePath);
    }
}

