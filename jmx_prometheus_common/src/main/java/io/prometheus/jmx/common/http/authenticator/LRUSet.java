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

package io.prometheus.jmx.common.http.authenticator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to implement a Set with LRU semantics
 *
 * @param <T> the object type
 */
public class LRUSet<T> {

    private final LinkedHashMap<T, Byte> linkedHashMap;

    /**
     * Constructor
     *
     * @param size size
     */
    public LRUSet(int size) {
        linkedHashMap =
                new LinkedHashMap<T, Byte>() {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry eldest) {
                        return size() > size;
                    }
                };
    }

    /**
     * Method to add an object to the set
     *
     * @param t the object to add to the set
     */
    public synchronized void add(T t) {
        linkedHashMap.put(t, (byte) 1);
    }

    /**
     * Method to return whether the set contains the object
     *
     * @param t the object
     * @return true if the set contains the object, else false
     */
    public synchronized boolean contains(T t) {
        return linkedHashMap.containsKey(t);
    }

    /**
     * Method to remove an object from the set
     *
     * @param t the object
     * @return true if the object existed and was removed, else false
     */
    public synchronized boolean remove(T t) {
        return linkedHashMap.remove(t) != null;
    }

    /**
     * Method to get the set size
     *
     * @return the set size
     */
    public synchronized int size() {
        return linkedHashMap.size();
    }
}
