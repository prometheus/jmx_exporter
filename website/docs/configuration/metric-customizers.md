---
title: Metric Customizers
---

`metricCustomizers` is a beta feature for adding labels or extra metrics for matching MBeans.

## Structure

| Field | Description |
| --- | --- |
| `metricCustomizers[]` | List of customizers. |
| `mbeanFilter` | Required filter for matching MBeans. |
| `mbeanFilter.domain` | Required MBean domain. |
| `mbeanFilter.properties` | Optional ObjectName properties map. |
| `attributesAsLabels` | Attribute names added as labels to existing or new metrics. Required when `extraMetrics` is absent. |
| `extraMetrics[]` | Extra metrics to create. Required when `attributesAsLabels` is absent. |
| `extraMetrics[].name` | Required extra metric name. |
| `extraMetrics[].value` | Required boolean or numeric value. |
| `extraMetrics[].description` | Optional help text. |

Each customizer must include `mbeanFilter` and at least one of `attributesAsLabels` or `extraMetrics`.

## Attributes as labels

```yaml
includeObjectNames:
- io.prometheus.jmx:type=customValue
metricCustomizers:
- mbeanFilter:
    domain: io.prometheus.jmx
    properties:
      type: customValue
  attributesAsLabels:
  - Text
rules:
- pattern: ".*"
```

## Extra metrics

```yaml
includeObjectNames:
- io.prometheus.jmx:type=stringValue
metricCustomizers:
- mbeanFilter:
    domain: io.prometheus.jmx
    properties:
      type: stringValue
  extraMetrics:
  - name: isActive
    value: true
    description: This scenario is active.
rules:
- pattern: ".*"
```
