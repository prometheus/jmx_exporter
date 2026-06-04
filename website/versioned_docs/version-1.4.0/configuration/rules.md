---
title: Rules
---

Rules convert scraped JMX attributes into Prometheus metric names, labels, values, and types.

## Rule fields

| Key | Description |
| --- | --- |
| `pattern` | Regex pattern matched against the formatted MBean and attribute input. Required when `name` is set. |
| `name` | Metric name. Capture groups can be referenced with `$1`, `$2`, and so on. |
| `value` | Static value or capture-group expression. |
| `valueFactor` | Numeric multiplier. Default `1.0`. |
| `labels` | Label map. Requires `name`. |
| `help` | Help text. Requires `name`. |
| `cache` | Cache rule match and mismatch results. Default `false`. |
| `type` | `GAUGE`, `COUNTER`, or `UNTYPED`. |
| `attrNameSnakeCase` | Convert attribute names to snake case. Default `false`. |
| `excludeJvmMetrics` | Exclude built-in JVM MBeans from collection when set to `true`. |

## Pattern input

Rule patterns match a generated string built from the MBean ObjectName, attribute name, and value path. Use exact examples from scraped output when building production rules.

## Default format

If no rule matches, attributes are exported using the default JMX Exporter naming behavior.

## Example

```yaml
rules:
- pattern: "java.lang<type=Memory><HeapMemoryUsage>Used"
  name: jvm_memory_heap_used_bytes
  type: GAUGE
```
