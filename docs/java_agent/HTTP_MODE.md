JMX Exporter Java agent / HTTP Mode
---

HTTP mode exposes metrics on an HTTP endpoint.

## Required binary

- `jmx_prometheus_javaagent-<VERSION>.jar`

## Installation

Example:

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=12345:exporter.yaml -jar YOUR_JAR.jar
```

Description:

- The JMX Exporter Java agent runs as part of your application
- Listens for metrics requests on all IP addresses on port `12345`

## Common Configuration

See [COMMON_CONFIGURATION.md](../COMMON_CONFIGURATION.md) for details.

## Optional Configuration

### HTTP Server Configuration 

TBD

### HTTP Authentication

TBD

### HTTPS Configuration

TBD

### Custom HTTP Authenticator Configuration

TBD
