---
title: Metric Customizers
---

Metric customizers add labels or extra metrics for selected MBeans.

## Structure

```yaml
metricCustomizers:
- mbeanFilter:
    domain: java.lang
    properties:
      type: Memory
  attributesAsLabels:
  - Name
  extraMetrics:
  - name: example_extra_metric
    value: 1
    description: Example extra metric
```

| Key | Description |
| --- | --- |
| `metricCustomizers[]` | Customizer list. |
| `mbeanFilter.domain` | Required domain. |
| `mbeanFilter.properties` | Optional ObjectName properties. |
| `attributesAsLabels` | Attribute names to add as labels. |
| `extraMetrics[].name` | Required extra metric name. |
| `extraMetrics[].value` | Required extra metric value. |
| `extraMetrics[].description` | Optional description. |

## Attributes as labels

Use `attributesAsLabels` when a selected MBean attribute should become a Prometheus label.

## Extra metrics

Use `extraMetrics` to emit additional static metrics for a selected MBean.
