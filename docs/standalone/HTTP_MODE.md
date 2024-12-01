Standalone JMX Exporter / HTTP Mode
---

HTTP mode exposes metrics on an HTTP endpoint.

## Required jar

- jmx_prometheus_standalone-\<VERSION>.jar

## Installation

Example:

```shell
java -jar jmx_prometheus_standalone-<VERSION>.jar <PORT> exporter.yaml
```

Description:

- The Standalone JMX Exporter runs as a separate application to your application and collects JMX MBean metrics via RMI
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
