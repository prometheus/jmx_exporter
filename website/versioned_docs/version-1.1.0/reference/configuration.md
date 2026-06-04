---
title: Configuration Reference
---

This reference lists the source-backed exporter YAML keys for JMX Exporter 1.1.0.

## Top-level collector keys

| Key | Description |
| --- | --- |
| `startDelaySeconds` | Delay before collection starts. Default `0`. |
| `lowercaseOutputName` | Convert metric names to lowercase. Default `false`. |
| `lowercaseOutputLabelNames` | Convert label names to lowercase. Default `false`. |

## Remote JMX connection keys

| Key | Description |
| --- | --- |
| `hostPort` | Remote host and port; converted to a default JMX service URL. Mutually exclusive with `jmxUrl`. |
| `jmxUrl` | Explicit JMX service URL. Mutually exclusive with `hostPort`. |
| `username` | Remote JMX username. |
| `password` | Remote JMX password. |
| `ssl` | Boolean for remote JMX/RMI SSL. |

## Object-name and attribute filters

| Key | Description |
| --- | --- |
| `includeObjectNames` | ObjectNames to query; defaults to all. |
| `excludeObjectNames` | ObjectNames not to query; takes precedence. |
| `whitelistObjectNames` | Compatibility alias for `includeObjectNames`. |
| `blacklistObjectNames` | Compatibility alias for `excludeObjectNames`. |
| `includeObjectNameAttributes` | Map of ObjectName strings to included attributes. |
| `excludeObjectNameAttributes` | Map of ObjectName strings to excluded attributes. |
| `autoExcludeObjectNameAttributes` | Automatically exclude unsupported attributes. Default `true`. |

## Rule keys

| Key | Description |
| --- | --- |
| `pattern` | Regex pattern. Required when `name` is set. |
| `name` | Metric name. |
| `value` | Static value or capture-group expression. |
| `valueFactor` | Numeric multiplier. Default `1.0`. |
| `labels` | Label map. Requires `name`. |
| `help` | Help text. Requires `name`. |
| `cache` | Cache rule match and mismatch results. Default `false`. |
| `type` | `GAUGE`, `COUNTER`, or `UNTYPED`. |
| `attrNameSnakeCase` | Convert attribute names to snake case. Default `false`. |

## HTTP server keys

| Key | Description |
| --- | --- |
| `httpServer.threads.minimum` | Minimum thread count. Default `1`; required if `threads` is configured. |
| `httpServer.threads.maximum` | Maximum thread count. Default `10`; required if `threads` is configured. |
| `httpServer.threads.keepAliveTime` | Keep-alive time in seconds. Default `120`; required if `threads` is configured. |

## HTTP authentication keys

| Key | Description |
| --- | --- |
| `httpServer.authentication.basic.username` | Required for Basic authentication. |
| `httpServer.authentication.basic.password` | Required for plaintext Basic authentication. |
| `httpServer.authentication.basic.algorithm` | Optional; defaults to plaintext. Supports `SHA-1`, `SHA-256`, `SHA-512`, and PBKDF2 algorithms. |
| `httpServer.authentication.basic.passwordHash` | Required for SHA and PBKDF2 algorithms. |
| `httpServer.authentication.basic.salt` | Required for SHA and PBKDF2 algorithms. |
| `httpServer.authentication.basic.iterations` | Optional positive integer for PBKDF2. |
| `httpServer.authentication.basic.keyLength` | Optional positive integer for PBKDF2. |
| `httpServer.authentication.plugin.class` | Custom authenticator class name. |
| `httpServer.authentication.plugin.subjectAttributeName` | Optional authenticated subject attribute name. |

## HTTP SSL/TLS keys

| Key | Description |
| --- | --- |
| `httpServer.ssl.keyStore.filename` | Key store file or `javax.net.ssl.keyStore` fallback. |
| `httpServer.ssl.keyStore.password` | Key store password or `javax.net.ssl.keyStorePassword` fallback. |
| `httpServer.ssl.certificate.alias` | Certificate alias. |

## OpenTelemetry keys

| Key | Description |
| --- | --- |
| `openTelemetry.endpoint` | Optional endpoint URL. |
| `openTelemetry.protocol` | Optional protocol. |
| `openTelemetry.interval` | Optional positive integer interval in seconds. |
| `openTelemetry.headers` | Optional string map. |
