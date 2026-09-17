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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guards the allocation-free bean name construction against the {@code toString()} + bracket
 * rewriting it replaced.
 *
 * <p>{@link JmxCollector.Receiver} builds the regex match name as
 * {@code domain + angleBrackets(beanProperties.toString()) + angleBrackets(attrKeys.toString())}.
 * {@code angleBrackets} strips the leading and trailing delimiters of the collection string, so the
 * inner contents must be reproduced exactly by
 * {@link JmxCollector.Receiver#appendMapContents(StringBuilder, LinkedHashMap)} and
 * {@link JmxCollector.Receiver#appendListContents(StringBuilder, List)}.
 */
public class BeanNameConstructionTest {

    private static String referenceInnerContents(Object collection) {
        String value = collection.toString();
        assertThat(value.length()).isGreaterThanOrEqualTo(2);
        return value.substring(1, value.length() - 1);
    }

    private static LinkedHashMap<String, String> mapOf(String... entries) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(entries[i], entries[i + 1]);
        }
        return map;
    }

    @Test
    void appendMapContentsMatchesToStringForEmptyMap() {
        LinkedHashMap<String, String> map = mapOf();
        StringBuilder builder = new StringBuilder();
        JmxCollector.Receiver.appendMapContents(builder, map);
        assertThat(builder.toString()).isEqualTo(referenceInnerContents(map));
    }

    @Test
    void appendMapContentsMatchesToStringForSingleEntry() {
        LinkedHashMap<String, String> map = mapOf("type", "Benchmark");
        StringBuilder builder = new StringBuilder();
        JmxCollector.Receiver.appendMapContents(builder, map);
        assertThat(builder.toString()).isEqualTo(referenceInnerContents(map));
    }

    @Test
    void appendMapContentsMatchesToStringForMultipleEntries() {
        LinkedHashMap<String, String> map = mapOf("service", "DataNode", "name", "DataNode-1", "type", "Activity");
        StringBuilder builder = new StringBuilder();
        JmxCollector.Receiver.appendMapContents(builder, map);
        assertThat(builder.toString()).isEqualTo(referenceInnerContents(map));
    }

    @Test
    void appendListContentsMatchesToStringForEmptyList() {
        List<String> list = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        JmxCollector.Receiver.appendListContents(builder, list);
        assertThat(builder.toString()).isEqualTo(referenceInnerContents(list));
    }

    @Test
    void appendListContentsMatchesToStringForSingleElement() {
        List<String> list = new ArrayList<>(Arrays.asList("key1"));
        StringBuilder builder = new StringBuilder();
        JmxCollector.Receiver.appendListContents(builder, list);
        assertThat(builder.toString()).isEqualTo(referenceInnerContents(list));
    }

    @Test
    void appendListContentsMatchesToStringForMultipleElements() {
        List<String> list = new ArrayList<>(Arrays.asList("key1", "key2", "key3"));
        StringBuilder builder = new StringBuilder();
        JmxCollector.Receiver.appendListContents(builder, list);
        assertThat(builder.toString()).isEqualTo(referenceInnerContents(list));
    }

    @Test
    void fullBeanNameMatchesLegacyConstruction() {
        String domain = "io.prometheus.jmx.test";
        LinkedHashMap<String, String> beanProperties =
                mapOf("service", "DataNode", "name", "DataNodeActivity-ams-hdd001-50010");
        List<String> attrKeys = new ArrayList<>(Arrays.asList("replaceBlockOp"));

        String legacy = domain
                + "<" + referenceInnerContents(beanProperties) + ">"
                + "<" + referenceInnerContents(attrKeys) + ">";

        StringBuilder builder = new StringBuilder();
        builder.append(domain).append('<');
        JmxCollector.Receiver.appendMapContents(builder, beanProperties);
        builder.append("><");
        JmxCollector.Receiver.appendListContents(builder, attrKeys);
        builder.append('>');

        assertThat(builder.toString()).isEqualTo(legacy);
        assertThat(legacy)
                .isEqualTo("io.prometheus.jmx.test<service=DataNode, name=DataNodeActivity-ams-hdd001-50010>"
                        + "<replaceBlockOp>");
    }

    @Test
    void mapWithNullValueMatchesToString() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("name", null);
        StringBuilder builder = new StringBuilder();
        JmxCollector.Receiver.appendMapContents(builder, map);
        assertThat(builder.toString()).isEqualTo(referenceInnerContents(map));
        assertThat(builder.toString()).isEqualTo("name=null");
    }

    @Test
    void mapWithNullKeyMatchesToString() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(null, "value");
        StringBuilder builder = new StringBuilder();
        JmxCollector.Receiver.appendMapContents(builder, new LinkedHashMap<>(map));
        assertThat(builder.toString()).isEqualTo(referenceInnerContents(map));
        assertThat(builder.toString()).isEqualTo("null=value");
    }
}
