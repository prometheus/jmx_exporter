---
title: HTTP Mode
weight: 2
---

HTTP Mode collects metric when accessed via HTTP and returns them as HTTP content. 

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

Reference HTTP Mode [Rules](http://localhost:1313/rules/rules/) for various `exporter.yaml` metrics configuration options.

# Metrics

1. Run your application.
2. Access HTTP Mode metrics using a browser to view your metrics.

```
http://<APPLICATION_HOSTNAME>:<PORT>/metrics
```

```
# HELP my_count_total example counter
# TYPE my_count_total counter
my_count_total{status="error"} 1.0
my_count_total{status="ok"} 2.0
```