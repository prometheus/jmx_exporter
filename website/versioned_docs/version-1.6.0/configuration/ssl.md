---
title: SSL
---

JMX Exporter has two SSL/TLS areas:

- `httpServer.ssl` configures HTTPS and optional mutual TLS for the exporter HTTP server.
- Top-level `ssl` configures remote JMX/RMI SSL for the collector connection used by the standalone exporter.

## Exporter HTTP TLS

```yaml
httpServer:
  ssl:
    keyStore:
      filename: keystore.jks
      type: JKS
      password: changeit
    certificate:
      alias: localhost
rules:
- pattern: ".*"
```

Supported HTTP TLS fields:

| Field | Description |
| --- | --- |
| `httpServer.ssl.keyStore.filename` | Key store file. Falls back to `javax.net.ssl.keyStore` if omitted. |
| `httpServer.ssl.keyStore.type` | Key store type. Falls back to `javax.net.ssl.keyStoreType` or the platform default. |
| `httpServer.ssl.keyStore.password` | Key store password. Falls back to `javax.net.ssl.keyStorePassword` if omitted. |
| `httpServer.ssl.certificate.alias` | Optional certificate alias. |
| `httpServer.ssl.protocols` | Optional comma-separated TLS protocols. |
| `httpServer.ssl.ciphers` | Optional comma-separated cipher suites. |

Exporter YAML values take precedence over Java system property fallbacks.

## Exporter HTTP mutual TLS

```yaml
httpServer:
  ssl:
    mutualTLS: true
    keyStore:
      type: PKCS12
      filename: keystore.pkcs12
      password: changeit
    trustStore:
      type: PKCS12
      filename: truststore.pkcs12
      password: changeit
    certificate:
      alias: localhost
rules:
- pattern: ".*"
```

Trust store fields mirror key store fields and fall back to `javax.net.ssl.trustStore`, `javax.net.ssl.trustStoreType`, and `javax.net.ssl.trustStorePassword` when omitted.

## Remote JMX/RMI SSL

For standalone remote JMX connections, top-level `ssl: true` enables SSL:

```yaml
hostPort: application.example.com:9999
ssl: true
rules:
- pattern: ".*"
```

The map form supports remote JMX key store and trust store settings plus comma-separated protocols and ciphers:

```yaml
hostPort: application.example.com:9999
ssl:
  enabled: true
  keyStore:
    filename: client.jks
    type: JKS
    password: changeit
  trustStore:
    filename: truststore.jks
    type: JKS
    password: changeit
  protocols: TLSv1.2,TLSv1.3
  ciphers: TLS_AES_128_GCM_SHA256
rules:
- pattern: ".*"
```
