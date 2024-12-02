Standalone JMX Exporter
---

The Standalone JMX Exporter runs as a separate application to your application and collects JMX MBean metrics via RMI

## Jar

- jmx_prometheus_standalone-\<VERSION>.jar

## Collection Modes

The Standalone JMX Exporter supports three modes of collection.

### HTTP mode

HTTP mode exposes metrics on an HTTP endpoint.

- "pull" model

#### Configuration

- [HTTP mode](HTTP_MODE.md)

### OpenTelemetry mode

OpenTelemetry mode pushes metrics to an OpenTelemetry endpoint.

- "push" model

#### Configuration

- [OpenTelemetry mode](OPEN_TELEMETRY_MODE.md)

### Combined mode

- HTTP
  - exposes metrics on an HTTP endpoint
  - "pull" model


- OpenTelemetry
  - pushes metrics to an OpenTelemetry endpoint
  - "push" model

See [Combined mode](COMBINED_MODE.md)

## Common Configuration

See [Common Configuration](../COMMON_CONFIGURATION.md) for details.