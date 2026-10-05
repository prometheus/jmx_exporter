---
title: HTTP Server Configuration
---

HTTP server configuration is read from the `httpServer` section of the exporter YAML. HTTP mode still must be enabled by the Java agent or standalone command arguments.

## Metrics path

The default metrics path is `/metrics`.

```yaml
httpServer:
  metrics:
    path: /custom/metrics
rules:
- pattern: ".*"
```

The path must be a non-blank string when configured.

## Thread pool

Defaults are `minimum: 1`, `maximum: 10`, and `keepAliveTime: 120` seconds.

```yaml
httpServer:
  threads:
    minimum: 1
    maximum: 10
    keepAliveTime: 120
rules:
- pattern: ".*"
```

When `threads` is configured, all three fields are required. Values must be integers greater than zero, and `maximum` must be greater than or equal to `minimum`. The server uses a blocking rejection handler for backpressure.

## Request deadline

`maximumRequestSeconds` optionally bounds how long the server waits for a request to complete.
When set, the in-flight exchange is closed after the configured number of seconds.

```yaml
httpServer:
  maximumRequestSeconds: 60
rules:
- pattern: ".*"
```

The value must be an integer greater than zero. When omitted, no deadline is applied.

## Related settings

- [Authentication](authentication)
- [SSL](ssl)
