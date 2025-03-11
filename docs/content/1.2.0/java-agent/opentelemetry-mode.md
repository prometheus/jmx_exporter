---
title: OpenTelemetry mode
weight: 3
---

OpenTelemetry mode periodically collects metrics and pushes them to an OpenTelemetry endpoint.

# Installation

### Example

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=<EXPORTER.YAML> -jar <YOUR_APPLICATION.JAR>
```

### Concrete Example

```shell
java -javaagent:jmx_prometheus_javaagent-1.2.0.jar=exporter.yaml -jar <YOUR_APPLICATION.JAR>
```

 **Notes**

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

OpenTelemetry mode also supports the use of `OTEL` environment variables.

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

#  Complex YAML Configuration Examples

 Integration tests  provide complex/concrete examples of application and YAML configuration files.

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
