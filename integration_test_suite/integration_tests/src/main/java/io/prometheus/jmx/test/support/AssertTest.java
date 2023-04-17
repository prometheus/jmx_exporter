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

package io.prometheus.jmx.test.support;

/**
 * Class to implement assert test methods
 */
public class AssertTest {

    /**
     * Constructor
     */
    private AssertTest() {
        // DO NOTHING
    }

    /**
     * Method to execute a test and return the test result
     *
     * @param test
     * @return the TestResult
     */
    public static TestResult assertTest(Test test) {
        return test.execute();
    }
}
