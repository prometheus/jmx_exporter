/*
 * Copyright 2023 Douglas Hoard
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

package io.prometheus.jmx.test.support;

import io.prometheus.jmx.test.HttpClient;

/**
 * Class to implement a healthy test
 */
public class HealthyTest extends BaseTest {

    private static final String CONTENT = "Exporter is Healthy.";

    public static final TestResult RESULT_200 = new TestResult().withCode(200).withContent(CONTENT);
    public static final TestResult RESULT_401 = new TestResult().withCode(401);

    /**
     * Constructor
     *
     * @param httpClient
     */
    public HealthyTest(HttpClient httpClient) {
        super(httpClient);
        withPath("/-/healthy");
    }
}
