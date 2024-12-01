Java agent JMX Exporter
---

The Java agent JMX Exporter runs in your application via a Java agent.

## Collection Modes

The Java agent JMX Exporter supports three modes of collection:

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
