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
java -jar jmx_prometheus_standalone-<VERSION>.jar [HOSTNAME:]<PORT> <EXPORTER.YAML>
```

 **Notes**

- `<PORT>` is required
- `[HOSTNAME]` is optional
  - if provided, must be separated from `<PORT>` using a colon (`:`) (e.g., `server:12345`)

### Concrete Example

```shell
java -jar jmx_prometheus_standalone-1.1.0.jar 12345 exporter.yaml
```

# Basic YAML Configuration

**exporter.yaml**

```yaml
openTelemetry:
  endpoint: http://prometheus:9090/api/v1/otlp
  protocol: http/protobuf
  interval: 60
hostPort: <APPLICATION_HOSTNAME_OR_IP>:<APPLICATION_RMI_PORT>
rules:
- pattern: ".*"
```

... or ...

```yaml
openTelemetry:
  endpoint: http://prometheus:9090/api/v1/otlp
  protocol: http/protobuf
  interval: 60
jmxUrl: service:jmx:rmi:///jndi/rmi://<APPLICATION_HOSTNAME_OR_IP>:<APPLICATION_RMI_PORT>/jmxrmi
rules:
- pattern: ".*"
```

### Additional RMI Configuration

#### RMI SSL

If your application's RMI server requires SSL you can add `ssl: true`

```yaml
hostPort: <APPLICATION_HOSTNAME_OR_IP>:<APPLICATION_RMI_PORT>
ssl: true
rules:
- pattern: ".*"
```

#### RMI Username / Password

If your application's RMI server requires authentication, you can add `username` and `password`

```yaml
hostPort: <APPLICATION_HOSTNAME_OR_IP>:<APPLICATION_RMI_PORT>
username: <APPLICATION_RMI_USERNAME>
password: <APPLICATION_RMI_PASSWORD>
rules:
- pattern: ".*"
```

#### Application RMI Configuration

Application RMI Configuration is complex. Reference Java documentation for configuration.

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
