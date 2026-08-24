---
title: Configuration Reference
---

This reference lists source-backed 1.6.0 exporter YAML keys. Guide pages explain common usage.

## Top-level collector keys

| Key | Description |
| --- | --- |
| `startDelaySeconds` | Non-negative startup delay before serving non-empty metrics. Default `0`. |
| `lowercaseOutputName` | Lowercase output metric names. Default `false`. |
| `lowercaseOutputLabelNames` | Lowercase output label names. Default `false`. |
| `inferCounterTypeFromName` | Infer counter type from metric names. Default `false`. |
| `rules` | Ordered rule list. If omitted, one default rule is used. |
| `excludeJvmMetrics` | Exclude common JVM ObjectNames when `true`; primarily for Java agent usage. |

## Remote JMX connection keys

| Key | Description |
| --- | --- |
| `hostPort` | Builds `service:jmx:rmi:///jndi/rmi://<hostPort>/jmxrmi`. Mutually exclusive with `jmxUrl`. |
| `jmxUrl` | Explicit JMX service URL. Mutually exclusive with `hostPort`. |
| `username` | Remote JMX username. Supports variable resolution. |
| `password` | Remote JMX password. Supports variable resolution. |
| `ssl` | Boolean or map for remote JMX/RMI SSL. |

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

## Metric customizer keys

| Key | Description |
| --- | --- |
| `metricCustomizers[]` | Beta customizer list. |
| `mbeanFilter.domain` | Required domain. |
| `mbeanFilter.properties` | Optional ObjectName properties. |
| `attributesAsLabels` | Attribute names to add as labels. |
| `extraMetrics[].name` | Required extra metric name. |
| `extraMetrics[].value` | Required extra metric value. |
| `extraMetrics[].description` | Optional description. |

## HTTP server keys

| Key | Description |
| --- | --- |
| `httpServer.metrics.path` | Metrics path. Default `/metrics`; must be non-blank when configured. |
| `httpServer.threads.minimum` | Minimum thread count. Default `1`; required if `threads` is configured. |
| `httpServer.threads.maximum` | Maximum thread count. Default `10`; required if `threads` is configured. |
| `httpServer.threads.keepAliveTime` | Keep-alive time in seconds. Default `120`; required if `threads` is configured. |

## HTTP authentication keys

| Key | Description |
| --- | --- |
| `httpServer.authentication.basic.username` | Required for Basic authentication. Supports variable resolution. |
| `httpServer.authentication.basic.password` | Required for plaintext Basic authentication. Supports variable resolution. |
| `httpServer.authentication.basic.algorithm` | Optional; defaults to plaintext. Supports `SHA-1`, `SHA-256`, `SHA-512`, and PBKDF2 algorithms. |
| `httpServer.authentication.basic.passwordHash` | Required for SHA and PBKDF2 algorithms. |
| `httpServer.authentication.basic.salt` | Required for SHA and PBKDF2 algorithms. |
| `httpServer.authentication.basic.iterations` | Optional positive integer for PBKDF2. |
| `httpServer.authentication.basic.keyLength` | Optional positive integer for PBKDF2. |
| `httpServer.authentication.plugin.class` | Custom authenticator class name. |
| `httpServer.authentication.plugin.subjectAttributeName` | Optional authenticated subject attribute name. |

## HTTP SSL/TLS keys

### Keystore identity

| Key | Description |
| --- | --- |
| `httpServer.ssl.keyStore.filename` | Key store file or `javax.net.ssl.keyStore` fallback. |
| `httpServer.ssl.keyStore.type` | Key store type or `javax.net.ssl.keyStoreType` fallback/platform default. |
| `httpServer.ssl.keyStore.password` | Key store password or `javax.net.ssl.keyStorePassword` fallback. |
| `httpServer.ssl.certificate.alias` | Certificate alias. Required for keystore mode. |

### PEM identity

| Key | Description |
| --- | --- |
| `httpServer.ssl.pem.certificate.filename` | PEM certificate chain file (leaf first). Required. |
| `httpServer.ssl.pem.privateKey.filename` | PEM private key file. Required. |
| `httpServer.ssl.pem.privateKey.password` | Optional password for encrypted private keys. Supports variable resolution. |

`httpServer.ssl.pem` and `httpServer.ssl.keyStore` are mutually exclusive.
`httpServer.ssl.certificate.alias` is not allowed with PEM mode.

### Shared TLS keys

| Key | Description |
| --- | --- |
| `httpServer.ssl.trustStore.filename` | Trust store file or `javax.net.ssl.trustStore` fallback. |
| `httpServer.ssl.trustStore.type` | Trust store type or `javax.net.ssl.trustStoreType` fallback/platform default. |
| `httpServer.ssl.trustStore.password` | Trust store password or `javax.net.ssl.trustStorePassword` fallback. |
| `httpServer.ssl.mutualTLS` | Enable client certificate authentication. Default `false`. |
| `httpServer.ssl.protocols` | Optional comma-separated protocols. |
| `httpServer.ssl.ciphers` | Optional comma-separated cipher suites. |

## OpenTelemetry keys

| Key | Description |
| --- | --- |
| `openTelemetry.endpoint` | Optional endpoint URL. |
| `openTelemetry.protocol` | Optional protocol. |
| `openTelemetry.interval` | Optional positive integer interval in seconds. |
| `openTelemetry.timeoutSeconds` | Optional positive integer timeout in seconds. |
| `openTelemetry.headers` | Optional string map. |
| `openTelemetry.resourceAttributes` | Optional string map. |
| `openTelemetry.serviceInstanceId` | Optional non-blank string. |
| `openTelemetry.serviceNamespace` | Optional non-blank string. |
| `openTelemetry.serviceName` | Optional non-blank string. |
| `openTelemetry.serviceVersion` | Optional non-blank string. |
