JMX Exporter / Java agent / Combined Mode
---

Both HTTP mode and OpenTelemetry mode are enabled.

## Required binary

- `jmx_prometheus_javaagent-<VERSION>.jar`

## Installation

Example:

```shell
java -javaagent:./jmx_prometheus_javaagent-<VERSION>.jar=exporter.yaml -jar YOUR_JAR.jar
```

Description:

- Runs the JMX Exporter Java agent as part of your application
- Port is used for HTTP mode.

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


