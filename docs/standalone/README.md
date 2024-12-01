Standalone JMX Exporter
---

The Standalone JMX Exporter runs as a separate application to your application, and collects JMX MBean metrics via RMI.

## Collection Modes

The Standalone JMX Exporter supports three modes of collection:

### HTTP mode

HTTP mode exposes metrics on an HTTP endpoint.

- "pull" model

See [HTTP_MODE.md](HTTP_MODE.md)

### OpenTelemetry mode

OpenTelemetry mode pushes metrics to an OpenTelemetry endpoint.

- "push" model

See [OPEN_TELEMETRY_MODE.md](OPEN_TELEMETRY_MODE.md)

### Combined mode

- HTTPS
  - exposes metrics on an HTTP endpoint
  - "pull" model


- OpenTelemetry - pushes metrics to an OpenTelemetry endpoint
  - pushes metrics to an OpenTelemetry endpoint
  - "push" model

See [COMBINED_MODE.md](COMBINED_MODE.md)

## Common Configuration

See [COMMON_CONFIGURATION.md](../COMMON_CONFIGURATION.md) for details.