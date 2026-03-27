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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MatchedRuleTest {

    private List<String> of(String... strings) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, strings);
        return list;
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructorWithValidNameSanitizesName() {
            MatchedRule rule = new MatchedRule(
                    "valid_name", "matchName", "GAUGE", "help text", of("label1"), of("value1"), 1.0, 1.0);

            assertThat(rule.name).isEqualTo("valid_name");
        }

        @Test
        void constructorWithNullNameDoesNotThrow() {
            MatchedRule rule =
                    new MatchedRule(null, "matchName", "GAUGE", "help text", of("label1"), of("value1"), 1.0, 1.0);
            assertThat((Object) rule).isNotNull();
        }

        @Test
        void constructorWithNullMatchName() {
            MatchedRule rule =
                    new MatchedRule("name", null, "GAUGE", "help text", of("label1"), of("value1"), 1.0, 1.0);

            assertThat(rule.matchName).isNull();
        }

        @Test
        void constructorWithNullType() {
            MatchedRule rule =
                    new MatchedRule("name", "matchName", null, "help text", of("label1"), of("value1"), 1.0, 1.0);

            assertThat(rule.type).isNull();
        }

        @Test
        void constructorWithNullHelp() {
            MatchedRule rule =
                    new MatchedRule("name", "matchName", "GAUGE", null, of("label1"), of("value1"), 1.0, 1.0);

            assertThat(rule.help).isNull();
        }

        @Test
        void constructorWithEmptyLabelLists() {
            MatchedRule rule = new MatchedRule(
                    "name",
                    "matchName",
                    "GAUGE",
                    "help text",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    1.0,
                    1.0);

            assertThat((Object) rule.labels).isNotNull();
        }

        @Test
        void constructorWithNullValue() {
            MatchedRule rule =
                    new MatchedRule("name", "matchName", "GAUGE", "help text", of("label1"), of("value1"), null, 1.0);

            assertThat(rule.value).isNull();
        }

        @Test
        void constructorWithZeroValueFactor() {
            MatchedRule rule =
                    new MatchedRule("name", "matchName", "GAUGE", "help text", of("label1"), of("value1"), 100.0, 0.0);

            assertThat(rule.valueFactor).isEqualTo(0.0);
        }

        @Test
        void constructorWithNegativeValueFactor() {
            MatchedRule rule =
                    new MatchedRule("name", "matchName", "GAUGE", "help text", of("label1"), of("value1"), 100.0, -2.5);

            assertThat(rule.valueFactor).isEqualTo(-2.5);
        }
    }

    @Nested
    class WithValueTests {

        @Test
        void withValueReturnsNewInstance() {
            MatchedRule original =
                    new MatchedRule("name", "matchName", "GAUGE", "help text", of("label1"), of("value1"), 1.0, 1.0);

            MatchedRule withNewValue = original.withValue(42.0);

            assertThat(withNewValue).isNotSameAs(original);
            assertThat(withNewValue.value).isEqualTo(42.0);
        }

        @Test
        void withValuePreservesOtherFields() {
            MatchedRule original = new MatchedRule(
                    "test_name", "test_matchName", "COUNTER", "test help", of("l1", "l2"), of("v1", "v2"), 10.0, 2.5);

            MatchedRule withNewValue = original.withValue(99.0);

            assertThat(withNewValue.name).isEqualTo("test_name");
            assertThat(withNewValue.matchName).isEqualTo("test_matchName");
            assertThat(withNewValue.type).isEqualTo("COUNTER");
            assertThat(withNewValue.help).isEqualTo("test help");
            assertThat((Object) withNewValue.labels).isNotNull();
            assertThat(withNewValue.valueFactor).isEqualTo(2.5);
            assertThat(withNewValue.value).isEqualTo(99.0);
        }

        @Test
        void withValuePreservesValueFactor() {
            MatchedRule original = new MatchedRule("name", "matchName", "GAUGE", "help", of(), of(), 1.0, 0.001);

            MatchedRule withNewValue = original.withValue(5000.0);

            assertThat(withNewValue.valueFactor).isEqualTo(0.001);
        }
    }

    @Nested
    class UnmatchedTests {

        @Test
        void unmatchedReturnsSingleton() {
            MatchedRule unmatched1 = MatchedRule.unmatched();
            MatchedRule unmatched2 = MatchedRule.unmatched();

            assertThat(unmatched1).isSameAs(unmatched2);
        }

        @Test
        void unmatchedInstanceIsUnmatched() {
            MatchedRule unmatched = MatchedRule.unmatched();

            assertThat(unmatched.isUnmatched()).isTrue();
            assertThat(unmatched.isMatched()).isFalse();
        }

        @Test
        void regularInstanceIsMatched() {
            MatchedRule matched = new MatchedRule("name", "matchName", "GAUGE", "help", of(), of(), 1.0, 1.0);

            assertThat(matched.isMatched()).isTrue();
            assertThat(matched.isUnmatched()).isFalse();
        }

        @Test
        void unmatchedHasNullFields() {
            MatchedRule unmatched = MatchedRule.unmatched();

            assertThat(unmatched.name).isNull();
            assertThat(unmatched.matchName).isNull();
            assertThat(unmatched.type).isNull();
            assertThat(unmatched.help).isNull();
            assertThat(unmatched.labels == null).isTrue();
            assertThat(unmatched.value).isNull();
            assertThat(unmatched.valueFactor).isEqualTo(1.0);
        }
    }

    @Nested
    class EqualsHashCodeTests {

        @Test
        void equalsReflexive() {
            MatchedRule rule = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            assertThat(rule).isEqualTo(rule);
        }

        @Test
        void equalsSymmetric() {
            MatchedRule rule1 = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            MatchedRule rule2 = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            assertThat(rule1).isEqualTo(rule2);
            assertThat(rule2).isEqualTo(rule1);
        }

        @Test
        void equalsWithDifferentValue() {
            MatchedRule rule1 = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            MatchedRule rule2 = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 2.0, 1.0);

            assertThat(rule1).isNotEqualTo(rule2);
        }

        @Test
        void equalsWithDifferentValueFactor() {
            MatchedRule rule1 = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            MatchedRule rule2 = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 2.0);

            assertThat(rule1).isNotEqualTo(rule2);
        }

        @Test
        void equalsWithDifferentName() {
            MatchedRule rule1 = new MatchedRule("name1", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            MatchedRule rule2 = new MatchedRule("name2", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            assertThat(rule1).isNotEqualTo(rule2);
        }

        @Test
        void equalsWithDifferentMatchName() {
            MatchedRule rule1 = new MatchedRule("name", "matchName1", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            MatchedRule rule2 = new MatchedRule("name", "matchName2", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            assertThat(rule1).isNotEqualTo(rule2);
        }

        @Test
        void equalsWithDifferentType() {
            MatchedRule rule1 = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            MatchedRule rule2 = new MatchedRule("name", "matchName", "COUNTER", "help", of("l1"), of("v1"), 1.0, 1.0);

            assertThat(rule1).isNotEqualTo(rule2);
        }

        @Test
        void equalsWithDifferentHelp() {
            MatchedRule rule1 = new MatchedRule("name", "matchName", "GAUGE", "help1", of("l1"), of("v1"), 1.0, 1.0);

            MatchedRule rule2 = new MatchedRule("name", "matchName", "GAUGE", "help2", of("l1"), of("v1"), 1.0, 1.0);

            assertThat(rule1).isNotEqualTo(rule2);
        }

        @Test
        void equalsWithNull() {
            MatchedRule rule = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            assertThat(rule).isNotEqualTo(null);
        }

        @Test
        void equalsWithDifferentClass() {
            MatchedRule rule = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            assertThat(rule).isNotEqualTo("not a MatchedRule");
        }

        @Test
        void hashCodeConsistentWithEquals() {
            MatchedRule rule1 = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            MatchedRule rule2 = new MatchedRule("name", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
        }

        @Test
        void hashCodeDifferentForDifferentObjects() {
            MatchedRule rule1 = new MatchedRule("name1", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            MatchedRule rule2 = new MatchedRule("name2", "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);

            assertThat(rule1.hashCode()).isNotEqualTo(rule2.hashCode());
        }
    }

    @Nested
    class ToStringTests {

        @Test
        void toStringContainsAllFields() {
            MatchedRule rule = new MatchedRule(
                    "test_name", "test_matchName", "GAUGE", "test help", of("l1", "l2"), of("v1", "v2"), 42.0, 2.5);

            String str = rule.toString();

            assertThat(str).contains("test_name");
            assertThat(str).contains("test_matchName");
            assertThat(str).contains("GAUGE");
            assertThat(str).contains("test help");
            assertThat(str).contains("42.0");
            assertThat(str).contains("2.5");
        }

        @Test
        void toStringHandlesNullFields() {
            MatchedRule unmatched = MatchedRule.unmatched();

            String str = unmatched.toString();

            assertThat(str).isNotNull();
            assertThat(str).contains("null");
        }
    }
}
