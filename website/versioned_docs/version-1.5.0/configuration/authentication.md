---
title: Authentication
---

HTTP mode supports HTTP Basic authentication and custom authenticator plugins.

## HTTP Basic authentication

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      password: secret
```

Supported password algorithms are plaintext, `SHA-1`, `SHA-256`, `SHA-512`, `PBKDF2WithHmacSHA1`, `PBKDF2WithHmacSHA256`, and `PBKDF2WithHmacSHA512`.

## Hashed passwords

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      algorithm: SHA-256
      salt: random
      passwordHash: 2bf7ed4906ac065bde39f7508d6102a6cdd7153a929ea883ff6cd04442772c99
```

PBKDF2 algorithms also support `iterations` and `keyLength`.

## Environment variables

HTTP Basic `username` and `password` support variable resolution:

```yaml
httpServer:
  authentication:
    basic:
      username: ${PROMETHEUS_USERNAME}
      password: ${PROMETHEUS_PASSWORD}
```

## Authenticator plugin

```yaml
httpServer:
  authentication:
    plugin:
      class: com.example.ExampleAuthenticator
      subjectAttributeName: subject
```

The plugin class must be available on the exporter classpath and must implement the authenticator contract used by JMX Exporter 1.5.0.
