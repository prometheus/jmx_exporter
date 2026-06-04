---
title: Configuration
---

JMX Exporter 1.2.0 uses an exporter YAML file. The same file can contain collector configuration, HTTP server configuration, and OpenTelemetry configuration.

## Minimal configuration

```yaml
rules:
- pattern: ".*"
```

## Configuration areas

| Area | Top-level keys | Details |
| --- | --- | --- |
| Collector | `startDelaySeconds`, `hostPort`, `jmxUrl`, `username`, `password`, `ssl`, `rules` | [Rules](rules) and [Object Names](object-names) |
| HTTP server | `httpServer` | [HTTP Server Configuration](http-server) |
| HTTP authentication | `httpServer.authentication` | [Authentication](authentication) |
| HTTP TLS | `httpServer.ssl` | [SSL](ssl) |
| OpenTelemetry | `openTelemetry` | [OpenTelemetry](opentelemetry) |
| Metric customizers | `metricCustomizers` | [Metric Customizers](metric-customizers) |

## Advanced skeleton

```yaml
startDelaySeconds: 0
lowercaseOutputName: false
lowercaseOutputLabelNames: false
includeObjectNames:
- "java.lang:type=Memory"
excludeObjectNames: []
rules:
- pattern: "java.lang<type=Memory><HeapMemoryUsage>Used"
  name: jvm_memory_heap_used_bytes
  type: GAUGE
```

Only document and use keys that are supported by the 1.2.0 implementation. The [configuration reference](../reference/configuration) lists the source-backed keys.
