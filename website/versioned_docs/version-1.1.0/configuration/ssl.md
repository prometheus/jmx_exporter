---
title: SSL
---

SSL configuration controls HTTPS for the exporter HTTP server. Remote JMX/RMI SSL for standalone mode uses top-level `ssl: true`.

## Exporter HTTP TLS

```yaml
httpServer:
  ssl:
    keyStore:
      filename: exporter.p12
      password: changeit
    certificate:
      alias: exporter
```



| Key | Description |
| --- | --- |
| `httpServer.ssl.keyStore.filename` | Key store file or `javax.net.ssl.keyStore` fallback. |
| `httpServer.ssl.keyStore.password` | Key store password or `javax.net.ssl.keyStorePassword` fallback. |
| `httpServer.ssl.certificate.alias` | Certificate alias. |

## Remote JMX/RMI SSL

For standalone remote JMX/RMI SSL, use the top-level boolean key:

```yaml
hostPort: application.example.com:9999
ssl: true
rules:
- pattern: ".*"
```
