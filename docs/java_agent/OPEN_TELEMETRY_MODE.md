JMX Exporter Java agent / OpenTelemetry Mode
---

OpenTelemetry mode periodically polls metrics and pushes them to an OpenTelemetry endpoint.

## Required jar

- jmx_prometheus_javaagent-\<VERSION>.jar

## Installation

Example:

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=<EXPORTER.YAML> -jar YOUR_JAR.jar
```

Description:

- The JMX Exporter Java agent runs as part of your application
- No port is provided

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


