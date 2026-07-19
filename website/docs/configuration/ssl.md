---
title: SSL
---

JMX Exporter has two SSL/TLS areas:

- `httpServer.ssl` configures HTTPS and optional mutual TLS for the exporter HTTP server.
- Top-level `ssl` configures remote JMX/RMI SSL for the collector connection used by the standalone exporter.

## Exporter HTTP TLS

The exporter supports two identity modes for HTTPS: Java keystore (JKS/PKCS12) and PEM files.
You choose one mode by populating the corresponding block under `httpServer.ssl`.
The two identity blocks are mutually exclusive.

### Keystore mode

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

Supported keystore fields:

| Field | Description |
| --- | --- |
| `httpServer.ssl.keyStore.filename` | Key store file. Falls back to `javax.net.ssl.keyStore` if omitted. |
| `httpServer.ssl.keyStore.type` | Key store type. Falls back to `javax.net.ssl.keyStoreType` or the platform default. |
| `httpServer.ssl.keyStore.password` | Key store password. Falls back to `javax.net.ssl.keyStorePassword` if omitted. |
| `httpServer.ssl.certificate.alias` | Certificate alias. Required for keystore mode. |
| `httpServer.ssl.protocols` | Optional comma-separated TLS protocols. |
| `httpServer.ssl.ciphers` | Optional comma-separated cipher suites. |

Exporter YAML values take precedence over Java system property fallbacks.

### PEM mode

Supply a PEM-encoded certificate chain and private key as separate files.
This avoids the need to convert to a Java keystore.

```yaml
httpServer:
  ssl:
    pem:
      certificate:
        filename: server.pem
      privateKey:
        filename: key.pem
        password: ${PRIVATE_KEY_PASSWORD}  # optional
rules:
- pattern: ".*"
```

Supported PEM fields:

| Field | Description |
| --- | --- |
| `httpServer.ssl.pem.certificate.filename` | PEM certificate chain file. The leaf certificate must be first, followed by any intermediates in order. |
| `httpServer.ssl.pem.privateKey.filename` | PEM private key file. Exactly one private key. |
| `httpServer.ssl.pem.privateKey.password` | Optional password for encrypted private keys. Supports `${ENV_VAR}` variable resolution. |

Supported private key formats:

- Unencrypted PKCS8 (`BEGIN PRIVATE KEY`)
- Traditional RSA (`BEGIN RSA PRIVATE KEY`)
- Traditional EC (`BEGIN EC PRIVATE KEY`)
- Encrypted PKCS8 (`BEGIN ENCRYPTED PRIVATE KEY`)
- Encrypted traditional RSA/EC

The private key must match the first certificate in the chain.

**Mutual exclusions:**
- `httpServer.ssl.pem` and `httpServer.ssl.keyStore` are mutually exclusive.
- `httpServer.ssl.certificate.alias` is not allowed with PEM mode.
- In PEM mode, `javax.net.ssl.keyStore*` system properties are ignored for identity material.

**Private key permissions:**
Restrict filesystem permissions on private key files.
For example, `chmod 600 key.pem`.

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

## Certificate reloading

The exporter automatically checks for updated certificate material every hour.

- **Keystore mode:** The keystore file is re-read when its content changes.
- **PEM mode:** Both the certificate and key files are checked. If either changes, both are reloaded together as an atomic unit.
- **Truststore:** When mutual TLS is enabled, the truststore is checked independently.

If a reload fails (for example, due to a malformed file or key/certificate mismatch), the last valid identity continues to be used.
The server does not stop.

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
