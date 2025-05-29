---
title: OpenTelemetry mode
weight: 3
---

OpenTelemetry mode periodically collects metrics and pushes them to an OpenTelemetry endpoint.

# Installation

### Example

```shell
-jar jmx_prometheus_standalone-<VERSION>.jar exporter.yaml
```

### Concrete Example

```shell
java -javaagent:jmx_prometheus_javaagent-1.4.0.jar exporter.yaml
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
Run the Standalone JMX Exporter application.

Access OpenTelemetry metrics using your OpenTelemetry platform.

#  Complex YAML Configuration Examples

 Integration tests  provide complex/concrete examples of application and YAML configuration files.

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
