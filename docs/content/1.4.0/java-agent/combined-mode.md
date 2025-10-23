---
title: Combined mode
weight: 4
---

Combined mode allows for both HTTP mode and OpenTelemetry mode metrics collection methods.

### HTTP mode

Exposes metric using an HTTP endpoint.

- metrics are collected when the HTTP endpoint is accessed
- "pull" model

### OpenTelemetry mode

Pushes metrics to an OpenTelemetry endpoint.

- metrics are periodically collected and pushed OpenTelemetry endpoint
- "push" model

**Notes**

-  Due to the independent collection methods, HTTP mode metrics most likely  will not match OpenTelemetry mode metrics exactly

# Installation

### Example

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=[HOSTNAME:]<PORT>:<EXPORTER.YAML> -jar <YOUR_APPLICATION.JAR>
```

 **Notes**

- `<PORT>` is required
- `[HOSTNAME]` is optional
  - if provided, must be separated from `<PORT>` using a colon (`:`) (e.g., `server:12345`)

### Concrete Example

```shell
java -javaagent:jmx_prometheus_javaagent-1.4.0.jar=12345:exporter.yaml -jar <YOUR_APPLICATION.JAR>
```

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

Reference HTTP mode [Rules](../../http-mode/rules/) for various `exporter.yaml` metrics configuration options.

# Metrics

1. Run your application.
2. Access HTTP mode metrics using a browser to view your metrics.

```
http://<APPLICATION_HOSTNAME_OR_IP>:<PORT>/metrics
```

```
# HELP my_count_total example counter
# TYPE my_count_total counter
my_count_total{status="error"} 1.0
my_count_total{status="ok"} 2.0
```

3. Access your OpenTelemetry platform to view OpenTelemetry metrics.

#  Complex YAML Configuration Examples

 Integration tests  provide complex/concrete examples of application and YAML configuration files.

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
