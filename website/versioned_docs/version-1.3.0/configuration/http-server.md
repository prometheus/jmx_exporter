---
title: HTTP Server Configuration
---

HTTP mode starts the exporter HTTP server and serves Prometheus metrics.

## Metrics path

The metrics path is `/metrics`. This version does not provide a YAML setting to change the metrics path.

## Thread pool

```yaml
httpServer:
  threads:
    minimum: 1
    maximum: 10
    keepAliveTime: 120
```

| Key | Description |
| --- | --- |
| `httpServer.threads.minimum` | Minimum thread count. Default `1`; required if `threads` is configured. |
| `httpServer.threads.maximum` | Maximum thread count. Default `10`; required if `threads` is configured. |
| `httpServer.threads.keepAliveTime` | Keep-alive time in seconds. Default `120`; required if `threads` is configured. |

If the work queue is full, the request is blocked until space is available.

## Related settings

- [Authentication](authentication) configures HTTP Basic authentication and authenticator plugins.
- [SSL](ssl) configures HTTPS and mutual TLS where supported.
