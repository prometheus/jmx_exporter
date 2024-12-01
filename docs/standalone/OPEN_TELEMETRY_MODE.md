Standalone JMX Exporter / OpenTelemetry Mode
---

OpenTelemetry mode periodically polls metrics and pushes them to an OpenTelemetry endpoint.

## Jar

- jmx_prometheus_standalone-\<VERSION>.jar

## Installation

Example:

```shell
java -jar jmx_prometheus_standalone-<VERSION>.jar <EXPORTER.YAML>
```

Description:

- The Standalone JMX Exporter runs as a separate application to your application and collects JMX MBean metrics via RMI
- No hostname or port is provided

## OpenTelemetry Configuration

OpenTelemetry must be configured in your exporter.yaml file...

### OpenTelemetry specific configuration

```yaml
openTelemetry:
  endpoint: http://prometheus:9090/api/v1/otlp
  protocol: http/protobuf
  interval: 60
```

### Working example

```yaml
openTelemetry:
  endpoint: http://prometheus:9090/api/v1/otlp
  protocol: http/protobuf
  interval: 60
rules:
  - pattern: ".*"
```

## Common Configuration

See [Common Configuration](../COMMON_CONFIGURATION.md) for details.
