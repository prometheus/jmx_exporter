Standalone JMX Exporter / Combined Mode
---

Both HTTP mode and OpenTelemetry mode are enabled.

## Required binary

- `jmx_prometheus_standalone-<VERSION>.jar`

## Installation

Example:

```shell
java -jar jmx_prometheus_standalone-<VERSION>.jar <PORT> exporter.yaml
```

Description:

- The Standalone JMX Exporter runs as a separate application to your application, and collects JMX MBean metrics via RMI 
- `<PORT>` is required for HTTP mode
- Listens for metrics requests on all IP addresses on port `<PORT>`

## OpenTelemetry Configuration

OpenTelemetry must be configured in your exporter.yaml file...

### OpenTelemetry specific configuration

```yaml
openTelemetry:
  endpoint: http://prometheus:9090/api/v1/otlp
  protocol: http/protobuf
  interval: 1
```

### Working example

```yaml
openTelemetry:
  endpoint: http://prometheus:9090/api/v1/otlp
  protocol: http/protobuf
  interval: 1
rules:
  - pattern: ".*"
```

## Common Configuration

See [COMMON_CONFIGURATION.md](../COMMON_CONFIGURATION.md) for details.


