---
title: Configuration Overview
---

JMX Exporter uses an exporter YAML file. The same file can contain collector configuration, HTTP server configuration, and OpenTelemetry configuration.

## Minimal configuration

```yaml
rules:
- pattern: ".*"
```

If `rules` is omitted, the collector uses one default rule and collects matching values with the default metric format.

## Configuration areas

| Area | YAML location | Documentation |
| --- | --- | --- |
| Collector and rules | top level, `rules` | [Rules](rules) |
| Object and attribute filters | top level | [Object names](object-names) |
| Metric customizers | `metricCustomizers` | [Metric customizers](metric-customizers) |
| HTTP server | `httpServer` | [HTTP server](http-server) |
| HTTP authentication | `httpServer.authentication` | [Authentication](authentication) |
| HTTP TLS/mTLS | `httpServer.ssl` | [SSL](ssl) |
| OpenTelemetry | `openTelemetry` | [OpenTelemetry](opentelemetry) |

## Advanced skeleton

```yaml
startDelaySeconds: 0
lowercaseOutputName: false
lowercaseOutputLabelNames: false

httpServer:
  metrics:
    path: /metrics
  threads:
    minimum: 1
    maximum: 10
    keepAliveTime: 120

openTelemetry:
  endpoint: http://localhost:4317

rules:
- pattern: ".*"
```

Only document and use keys that are supported by the implementation. The [configuration reference](../reference/configuration) lists the source-backed keys.
