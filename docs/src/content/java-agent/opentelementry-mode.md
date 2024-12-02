---
title: OpenTelemetry Mode
weight: 3
---

OpenTelemetry Mode periodically collects metrics and pushes them to an OpenTelemetry endpoint.

# Installation

### Example

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=<EXPORTER.YAML> -jar <YOUR_APPLICATION.JAR>
```

### Concrete Example

```shell
java -javaagent:jmx_prometheus_javaagent-1.1.0.jar=exporter.yaml -jar <YOUR_APPLICATION.JAR>
```

**NOTES**

- No `<HOSTNAME>` or `<PORT>` is used

# Basic YAML Configuration

**exporter.yaml**

```yaml
openTelemetry:
  endpoint: http://prometheus:9090/api/v1/otlp
  protocol: http/protobuf
  interval: 60
rules:
- pattern: ".*"
```

# Advanced YAML Configuration

OpenTelemetry Mode also supports the use of `OTEL` environment variables.

**exporter.yaml**

```yaml
openTelemetry:
  # endpoint defined via environment variable "OTEL_EXPORTER_OTLP_ENDPOINT"
  # protocol defined via environment variable "OTEL_EXPORTER_OTLP_PROTOCOL"
  # interval defined via environment variable "OTEL_METRIC_EXPORT_INTERVAL"
rules:
- pattern: ".*"
```

# Output

Run your application.

Access OpenTelemetry metrics using your OpenTelemetry platform.
