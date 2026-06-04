---
title: Authentication
---

Authentication settings under `httpServer.authentication` protect the exporter HTTP server. Remote JMX authentication uses top-level `username` and `password` instead; see [Standalone exporter](../deployment/standalone).

## HTTP Basic authentication

Plaintext password configuration:

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      password: secret
rules:
- pattern: ".*"
```

Use TLS when sending Basic authentication credentials.

## Hashed passwords

Supported `algorithm` values include `SHA-1`, `SHA-256`, `SHA-512`, `PBKDF2WithHmacSHA1`, `PBKDF2WithHmacSHA256`, and `PBKDF2WithHmacSHA512`.

SHA algorithms require `passwordHash` and `salt`:

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      algorithm: SHA-256
      salt: 98LeBWIjca
      passwordHash: 66:5E:DC:0B:04:E9:52:67:64:FC:8F:51:66:72:70:BB
rules:
- pattern: ".*"
```

PBKDF2 algorithms require `passwordHash` and `salt`. `iterations` and `keyLength` are optional; implementation defaults are used when omitted.

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      algorithm: PBKDF2WithHmacSHA256
      salt: 98LeBWIjca
      passwordHash: 02:56:48:21:BA:EF:62:CC:92:79:90:C4:E6:45:F0:48
rules:
- pattern: ".*"
```

## Environment variables

`username` and plaintext `password` values support variable resolution:

```yaml
httpServer:
  authentication:
    basic:
      username: ${USERNAME}
      password: ${SECRET}
rules:
- pattern: ".*"
```

## Authenticator plugin

A custom plugin must be loadable by class name and implement `com.sun.net.httpserver.Authenticator`.

```yaml
httpServer:
  authentication:
    plugin:
      class: io.prometheus.jmx.AuthenticatorPlugin
      subjectAttributeName: io.prometheus.jmx.CustomAuthenticatorSubjectAttribute
rules:
- pattern: ".*"
```
