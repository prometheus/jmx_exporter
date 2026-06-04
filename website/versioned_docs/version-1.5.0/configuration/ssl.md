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

 HTTP SSL keystore and truststore passwords support variable resolution in this version.

| Key | Description |
| --- | --- |
| `httpServer.ssl.keyStore.filename` | Key store file or `javax.net.ssl.keyStore` fallback. |
| `httpServer.ssl.keyStore.type` | Optional key store type. |
| `httpServer.ssl.keyStore.password` | Key store password or `javax.net.ssl.keyStorePassword` fallback. |
| `httpServer.ssl.certificate.alias` | Certificate alias. |
| `httpServer.ssl.trustStore.filename` | Trust store file for mutual TLS. |
| `httpServer.ssl.trustStore.type` | Optional trust store type. |
| `httpServer.ssl.trustStore.password` | Trust store password. |
| `httpServer.ssl.mutualTLS` | Enable client certificate authentication. Default `false`. |

## Exporter HTTP mutual TLS

```yaml
httpServer:
  ssl:
    mutualTLS: true
    keyStore:
      filename: exporter.p12
      password: changeit
    trustStore:
      filename: clients.p12
      password: changeit
```

## Remote JMX/RMI SSL

For standalone remote JMX/RMI SSL, use the top-level boolean key:

```yaml
hostPort: application.example.com:9999
ssl: true
rules:
- pattern: ".*"
```
