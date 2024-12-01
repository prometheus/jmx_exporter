JMX Exporter Java agent
---

The JMX Exporter Java agent runs in your application as a Java agent.

## Required jar

- jmx_prometheus_javaagent-\<VERSION>.jar

## Collection Modes

The JMX Exporter Java agent supports three modes of collection.

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

- HTTPS
  - exposes metrics on an HTTP endpoint
  - "pull" model


- OpenTelemetry
  - pushes metrics to an OpenTelemetry endpoint
  - "push" model

#### Configuration

- [Combined mode](COMBINED_MODE.md)

## Common Configuration

See [Common Configuration](../COMMON_CONFIGURATION.md) for details.