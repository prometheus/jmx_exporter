---
title: Authentication
weight: 2
---

HTTP mode supports configuring HTTP BASIC authentication as well as use of a custom authenticator plugin.

## HTTP BASIC Authentication

HTTP BASIC authentication supports using the following configuration algorithms:

- plaintext - plaintext password
- SHA-1 - SHA-1(`<salt>:<password>`)
- SHA-256 - SHA-256(`<salt>:<password>`)
- SHA-512 - SHA-512(`<salt>:<password>`)
- PBKDF2WithHmacSHA1
- PBKDF2WithHmacSHA256
- PBKDF2WithHmacSHA512

---

Plaintext example:

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      password: secret
```

---

SHA-256 example using a salted password SHA-256(`<salt>:<password>`) with a password of `secret`

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      passwordHash: 2bf7ed4906ac065bde39f7508d6102a6cdd7153a929ea883ff6cd04442772c99
      algorithm: SHA-256
      salt: U9i%=N+m]#i9yvUV:bA/3n4X9JdPXf=n
```

---

PBKDF2WithHmacSHA256 example with a password of `secret`

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      passwordHash: A1:0E:4E:62:F7:1E:0B:59:0A:32:EA:CC:7C:65:37:1F:6D:A6:F1:F1:ED:3F:73:ED:C9:65:19:37:21:5B:6D:4E:9D:C6:61:DF:B5:BF:BB:16:B8:9A:50:14:57:CE:3D:14:67:73:A3:71:1B:87:3B:C4:B1:0E:DC:2D:0B:10:65:D6:F5:B6:DA:07:DD:EE:DA:AC:9C:60:CD:B4:59:0C:C9:CB:A7:3D:7E:30:3E:43:83:E9:E4:13:34:A1:F1:87:5C:24:46:8E:13:90:A6:66:E1:A6:F3:0B:5A:E7:14:8A:98:6A:81:2B:B6:F8:EF:95:D4:82:7E:FB:5E:2D:D3:24:FE:96
      algorithm: PBKDF2WithHmacSHA256
      salt: U9i%=N+m]#i9yvUV:bA/3n4X9JdPXf=n
```

- iterations = `600000` (default value for PBKDF2WithHmacSHA256 )
- keyLength = `128` bits (default value)

**Notes**

- PBKDF2WithHmacSHA1 default iterations = `1300000`
- PBKDF2WithHmacSHA256 default iterations = `600000`
- PBKDF2WithHmacSHA512 default iterations = `210000`
- default keyLength = `128` (bits)

### Generation of `passwordHash`

- `sha1sum`, `sha256sum`, and `sha512sum` can be used to generate the `passwordHash`
- `openssl` can be used to generate a PBKDF2WithHmac based algorithm `passwordHash`

# Pluggable Authenticator

It is possible to use a customer pluggable authenticator (`com.sun.net.httpserver.Authenticator`) implementation for the **Java agent**.

The custom pluggable authenticator class needs to be on the jvm classpath.

The class name to load is provided through `authentication/plugin` configuration as follows:

```yaml
httpServer:
  authentication:
    plugin:
       class: my.custom.AuthenticatorWithNoArgConstructor
```

If the custom pluggable authenticator needs to provide an authenticated Subject visible to the application, it can set a named attribute on the HttpExchange with that subject.

The agent will arrange that subsequent calls occur in a `Subject.doAs()`.

The name of the attribute must be provided through `subjectAttributeName` configuration as follows:

```yaml
httpServer:
  authentication:
    plugin:
       class: my.custom.AuthenticatorWithNoArgConstructorThatSetsASubjectAttribute
       subjectAttributeName: "custom.subject.for.doAs");
```

#  Complex YAML Configuration Examples

Integration tests  provide complex/concrete examples of application and YAML configuration files.

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
