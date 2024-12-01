JMX Exporter Java agent / HTTP Mode
---

HTTP mode exposes metrics on an HTTP endpoint.

## Required binary

- `jmx_prometheus_javaagent-<VERSION>.jar`

## Installation

Example:

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=<PORT>:exporter.yaml -jar YOUR_JAR.jar
```

Description:

- The JMX Exporter Java agent runs as part of your application
- `<PORT>` is required
- Listens for metrics requests on all IP addresses on port `<PORT>`

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
