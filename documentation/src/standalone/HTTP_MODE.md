Standalone JMX Exporter / HTTP Mode
---

HTTP mode exposes metrics on an HTTP endpoint.

## Jar

- jmx_prometheus_standalone-\<VERSION>.jar

## Installation

Example:

```shell
java -jar jmx_prometheus_standalone-<VERSION>.jar [HOSTNAME:]<PORT> <EXPORTER.YAML>
```

Description:

- The Standalone JMX Exporter runs as a separate application to your application and collects JMX MBean metrics via RMI
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
