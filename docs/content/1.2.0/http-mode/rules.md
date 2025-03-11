---
title: Rules
weight: 1
---

HTTP mode rules that apply to both the JMX Exporter Java agent and Standalone JMX Exporter.

Name     | Description
---------|------------
startDelaySeconds | start delay before serving requests. Any requests within the delay period will result in an empty metrics set.
lowercaseOutputName | Lowercase the output metric name. Applies to default format and `name`. Defaults to false.
lowercaseOutputLabelNames | Lowercase the output metric label names. Applies to default format and `labels`. Defaults to false.
includeObjectNames | A list of [ObjectNames](https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html) to query. Defaults to all mBeans.
excludeObjectNames | A list of [ObjectNames](https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html) to not query. Takes precedence over `includeObjectNames`. Defaults to none.
autoExcludeObjectNameAttributes | Whether to auto exclude [ObjectName](https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html) attributes that can't be converted to standard metrics types. Defaults to `true`.
excludeObjectNameAttributes | A Map of [ObjectNames](https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html) with a list of attribute names to exclude. ObjectNames must be in canonical form. Both ObjectNames and attribute names are matched as a Strings (no regex.) Optional.
includeObjectNameAttributes | A Map of [ObjectNames](https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html) with a list of attribute names to include. ObjectNames must be in canonical form. Both ObjectNames and attribute names are matched as a Strings (no regex.) Optional.
rules      | A list of rules to apply in order, processing stops at the first matching rule. Attributes that aren't matched aren't collected. If not specified, defaults to collecting everything in the default format.
pattern           | Regex pattern to match against each bean attribute. The pattern is not anchored. Capture groups can be used in other options. Defaults to matching everything.
attrNameSnakeCase | Converts the attribute name to snake case. This is seen in the names matched by the pattern and the default format. For example, anAttrName to an\_attr\_name. Defaults to false.
name              | The metric name to set. Capture groups from the `pattern` can be used. If not specified, the default format will be used. If it evaluates to empty, processing of this attribute stops with no output. An Additional suffix may be added to this name (e.g `_total` for type `COUNTER`)
value             | Value for the metric. Static values and capture groups from the `pattern` can be used. If not specified the scraped mBean value will be used.
valueFactor       | Optional number that `value` (or the scraped mBean value if `value` is not specified) is multiplied by, mainly used to convert mBean values from milliseconds to seconds.
labels            | A map of label name to label value pairs. Capture groups from `pattern` can be used in each. `name` must be set to use this. Empty names and values are ignored. If not specified and the default format is not being used, no labels are set.
help              | Help text for the metric. Capture groups from `pattern` can be used. `name` must be set to use this. Defaults to the mBean attribute description, domain, and name of the attribute.
cache             | Whether to cache bean name expressions to rule computation (match and mismatch). Not recommended for rules matching on bean value, as only the value from the first scrape will be cached and re-used. This can increase performance when collecting a lot of mbeans. Defaults to `false`.
type              | The type of the metric, can be `GAUGE`, `COUNTER` or `UNTYPED`. `name` must be set to use this. Defaults to `UNTYPED`.
metricCustomizers | A list of objects that contain `mbeanFilter`, and at least one of `attributesAsLabels` and `extraMetrics`. For those mBeans that match the filter, the items in the `attributesAsLabels` list will be added as attributes to the existing, or new metrics, and items in the `extraMetrics` will generate new metrics.</p>`metricCustomizers` in BETA.
mbeanFilter | A map of the criteria by which mBeans are filtered. It contains `domain` and `properties`.
domain | Domain of an mBean. Mandatory if `metricCustomizers` defined.
properties | Properties of an mBean. Optional
attributesAsLabels | List of elements to be added as attributes to existing metrics. Mandatory if `metricCustomizers` defined, and `extraMetrics` not.
extraMetrics | A list of map of elements in order to create a new metric. It contains `name`, `value` and `description`. Mandatory if `metricCustomizers` defined, and `attributesAsLabels` not.
name | The name of the new metric. Mandatory if `extraMetrics` defined.
value | The value of the new metric. It needs to be boolean or number. Mandatory if `extraMetrics` defined.
description | The description of the new metric. Used in the HELP section of the logs and indicates what purpose the metric serves. Optional

Metric names and label names are sanitized. All characters other than `[a-zA-Z0-9:_]` are replaced with underscores,
and adjacent underscores are collapsed. There's no limitations on label values or the help text.

A minimal config is `{}`, which will connect to the local JVM and collect everything in the default format.
Note that the scraper always processes all mBeans, even if they're not exported.

 **Notes**

Both `whitelistObjectNames` and `blacklistObjectNames` are still supported for backward compatibility, but should be considered deprecated.

### Pattern input
The format of the input matches against the pattern is
```
domain<beanpropertyName1=beanPropertyValue1, beanpropertyName2=beanPropertyValue2, ...><key1, key2, ...>attrName: value
```

Part     | Description
---------|------------
domain   | Bean name. This is the part before the colon in the JMX object name.
beanPropertyName/Value | Bean properties. These are the key/values after the colon in the JMX object name.
keyN     | If composite or tabular data is encountered, the name of the attribute is added to this list.
attrName | The name of the attribute. For tabular data, this will be the name of the column. If `attrNameSnakeCase` is set, this will be converted to snake case.
value    | The value of the attribute.

No escaping or other changes are made to these values, with the exception of if `attrNameSnakeCase` is set.
The default help includes this string, except for the value.

### Default format

The default format will transform beans in a way that should produce sane metrics in most cases. It is
```
domain_beanPropertyValue1_key1_key2_...keyN_attrName{beanpropertyName2="beanPropertyValue2", ...}: value
```
If a given part isn't set, it'll be excluded.

#  Complex YAML Configuration Examples

Integration tests  provide complex/concrete examples of application and YAML configuration files.

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
