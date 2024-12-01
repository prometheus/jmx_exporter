JMX Exporter Java agent / HTTP Mode
---

HTTP mode exposes metrics on an HTTP endpoint.

## Jar

- jmx_prometheus_javaagent-\<VERSION>.jar

## Installation

Example:

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=<PORT>:<EXPORTER.YAML> -jar <YOUR_APPLICATION.JAR>
```

Description:

- The JMX Exporter Java agent runs as part of your application
- `<PORT>` is required
- `<HOSTNAME>` is optional

## Common Configuration

See [Common Configuration](../COMMON_CONFIGURATION.md) for details.

## Optional Configuration

### HTTP Server Configuration 

TBD

### HTTP Authentication

TBD

### HTTPS Configuration

TBD

### Custom HTTP Authenticator Configuration

TBD
