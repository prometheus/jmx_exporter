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

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class TestResult {

    private int code;
    private String contentType;
    private String content;

    private String resultContentType;
    private String resultContent;

    public TestResult(int code, String contentType, String content) {
        this.code = code;
        this.contentType = contentType;
        this.content = content;
    }

    public TestResult expect(int code, String contentType, String content) {
        assertThat(code).isEqualTo(this.code);

        if (this.contentType != null) {
            assertThat(contentType).isEqualTo(this.contentType);
        }

        if (this.content != null) {
            assertThat(content).isEqualTo(this.content);
        }

        return this;
    }

    public void dispatch(Consumer<String> consumer) {
        if (consumer != null) {
            consumer.accept(resultContent);
        }
    }

    String contentType() {
        return contentType;
    }

    void contentType(String contentType) {
        resultContentType = contentType;
    }

    void content(String content) {
        resultContent = content;
    }
}
