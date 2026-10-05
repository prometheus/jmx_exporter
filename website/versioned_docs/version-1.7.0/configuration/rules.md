---
title: Rules
---

Rules transform JMX MBean attributes into Prometheus metrics. Rules are evaluated in order and processing stops at the first matching rule. Attributes that do not match any configured rule are not collected.

## Rule fields

| Field | Description |
| --- | --- |
| `pattern` | Regular expression matched against the rule input. Capture groups can be used by other fields. Required when `name` is set. |
| `name` | Metric name. Capture groups from `pattern` can be used. If omitted, the default format is used. |
| `value` | Static value or capture-group expression. If omitted, the scraped MBean value is used. |
| `valueFactor` | Number multiplied by the selected value. Useful for unit conversion. Default is `1.0`. |
| `labels` | Map of label names to label values. `name` must also be set. |
| `help` | Metric help text. `name` must also be set. |
| `cache` | Cache match and mismatch results for this rule. Default is `false`. Do not use for rules that match bean values. |
| `type` | Metric type. Supported values include `GAUGE`, `COUNTER`, and `UNTYPED`; `UNTYPED` maps to the implementation's unknown type. |
| `attrNameSnakeCase` | Convert the attribute name to snake case before matching and default formatting. Default is `false`. |

`help` or `labels` without `name` is invalid. `name` without `pattern` is invalid.

## Pattern input

Rules match this input shape:

```text
domain<beanPropertyName1=beanPropertyValue1, beanPropertyName2=beanPropertyValue2, ...><key1, key2, ...>attrName: value
```

| Part | Description |
| --- | --- |
| `domain` | ObjectName domain before the colon. |
| `beanPropertyName/Value` | ObjectName properties after the colon. |
| `keyN` | Composite or tabular data path elements. |
| `attrName` | Attribute name, or tabular column name. |
| `value` | Attribute value. |

## Default format

When a rule matches without `name`, the collector uses the default format:

```text
domain_beanPropertyValue1_key1_key2_...keyN_attrName{beanPropertyName2="beanPropertyValue2", ...} value
```

Metric names and label names are sanitized by the metrics library. Invalid characters are replaced or normalized for Prometheus compatibility.

## Examples

Collect everything with the default format:

```yaml
rules:
- pattern: ".*"
```

Create a named metric with labels:

```yaml
rules:
- pattern: 'java.lang<type=Memory><HeapMemoryUsage>used: (.*)'
  name: jvm_heap_memory_used_bytes
  value: '$1'
  labels:
    area: heap
  help: Used heap memory in bytes.
  type: GAUGE
```

Convert milliseconds to seconds:

```yaml
rules:
- pattern: '.*ProcessingTimeMillis: (.*)'
  name: application_processing_time_seconds
  value: '$1'
  valueFactor: 0.001
  type: GAUGE
```

Counter example:

```yaml
rules:
- pattern: '.*RequestCount: (.*)'
  name: application_requests_total
  value: '$1'
  type: COUNTER
```
