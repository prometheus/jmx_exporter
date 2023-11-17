/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LRUSetTest {

    @Test
    public void test() {
        LRUSet<Credentials> lruSet = new LRUSet<>(10);

        String username = "prometheus";
        String password = "secret";

        Credentials credentials = new Credentials(username, password);
        lruSet.add(credentials);

        assertTrue(lruSet.contains(credentials));
        assertTrue(lruSet.contains(new Credentials(username, password)));

        assertTrue(lruSet.remove(new Credentials(username, password)));
        assertFalse(lruSet.contains(new Credentials(username, password)));

        for (int i = 1; i <= 20; i++) {
            lruSet.add(new Credentials(username, password + i));

            if (i <= 10) {
                assertEquals(i, lruSet.size());
            } else {
                assertEquals(10, lruSet.size());
            }
        }
    }
}
