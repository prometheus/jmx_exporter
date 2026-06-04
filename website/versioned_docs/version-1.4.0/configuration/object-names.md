---
title: Object Names
---

Object-name and attribute filters limit which MBeans and attributes are collected.

## MBean filters

```yaml
includeObjectNames:
- "java.lang:type=Memory"
excludeObjectNames:
- "java.lang:type=ClassLoading"
```

| Key | Description |
| --- | --- |
| `includeObjectNames` | ObjectNames to query; defaults to all. |
| `excludeObjectNames` | ObjectNames not to query; takes precedence. |
| `whitelistObjectNames` | Compatibility alias for `includeObjectNames`. |
| `blacklistObjectNames` | Compatibility alias for `excludeObjectNames`. |

## Attribute filters

```yaml
includeObjectNameAttributes:
  "java.lang:type=Memory": ["HeapMemoryUsage"]
excludeObjectNameAttributes:
  "java.lang:type=Memory": ["NonHeapMemoryUsage"]
```

| Key | Description |
| --- | --- |
| `includeObjectNameAttributes` | Map of ObjectName strings to included attributes. |
| `excludeObjectNameAttributes` | Map of ObjectName strings to excluded attributes. |
| `autoExcludeObjectNameAttributes` | Automatically exclude unsupported attributes. Default `true`. |

## JVM metric exclusion

Set `excludeJvmMetrics: true` to add the built-in JVM MBean domains to the exclusion list. This key is supported in JMX Exporter 1.4.0.
