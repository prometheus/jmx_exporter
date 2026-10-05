---
title: Object Names and Attribute Filters
---

ObjectName filters limit which MBeans and attributes the collector processes.

## MBean filters

| Key | Description |
| --- | --- |
| `includeObjectNames` | List of `ObjectName` patterns to query. Defaults to all MBeans. |
| `excludeObjectNames` | List of `ObjectName` patterns not to query. Takes precedence over includes. Defaults to none. |
| `whitelistObjectNames` | Compatibility alias for `includeObjectNames`; supported but discouraged. |
| `blacklistObjectNames` | Compatibility alias for `excludeObjectNames`; supported but discouraged. |
| `excludeJvmMetrics` | When `true`, adds common JVM ObjectName patterns to the exclude list. Only relevant for the Java agent. |

```yaml
includeObjectNames:
- java.lang:type=Memory
- java.lang:type=GarbageCollector,name=*
excludeObjectNames:
- java.lang:type=MemoryPool,name=CodeHeap*
rules:
- pattern: ".*"
```

## Attribute filters

| Key | Description |
| --- | --- |
| `includeObjectNameAttributes` | Map of ObjectName strings to attributes to include. |
| `excludeObjectNameAttributes` | Map of ObjectName strings to attributes to exclude. |
| `autoExcludeObjectNameAttributes` | Automatically exclude attributes that cannot be converted to metrics. Default is `true`. |

ObjectName keys are parsed as `javax.management.ObjectName` values. Attribute names are matched as strings.

```yaml
includeObjectNameAttributes:
  "java.lang:type=Memory":
  - HeapMemoryUsage
excludeObjectNameAttributes:
  "java.lang:type=Threading":
  - AllThreadIds
autoExcludeObjectNameAttributes: true
rules:
- pattern: ".*"
```
