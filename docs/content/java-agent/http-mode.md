---
title: HTTP Mode
weight: 2
---

HTTP Mode collects metrics when accessed via HTTP, and returning them as HTTP content. 

# Installation

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=[HOSTNAME:]<PORT>:<EXPORTER.YAML> -jar <YOUR_APPLICATION.JAR>
```

**NOTES**

- `<PORT>` is required
- `[HOSTNAME]` is optional
  - if provided, must be separated from `<PORT>` using a colon (`:`) (e.g., `myserver:12345`)

### Concrete Example

```shell
java -javaagent:jmx_prometheus_javaagent-1.1.0.jar=12345:exporter.yaml -jar <YOUR_APPLICATION.JAR>
```

# Basic YAML Configuration

**exporter.yaml**

```yaml
rules:
- pattern: ".*"
```

# Advanced YAML Configuration

Reference HTTP Mode [Rules](/rules/rules/) for various `exporter.yaml` metrics configuration options.

# Metrics

1. Run your application.
2. Access HTTP Mode metrics using a browser to view your metrics.

```
http://<APPLICATION_HOSTNAME_OR_IP>:<PORT>/metrics
```

```
# HELP my_count_total example counter
# TYPE my_count_total counter
my_count_total{status="error"} 1.0
my_count_total{status="ok"} 2.0
```

#  Complex YAML Configuration Examples

 Integration tests provide complex and concrete examples of application and YAML configuration files.

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
