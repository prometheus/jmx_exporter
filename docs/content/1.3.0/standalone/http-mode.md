---
title: HTTP mode
weight: 2
---

HTTP mode collects metrics when accessed via HTTP, and returning them as HTTP content.

# Installation

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=[HOSTNAME:]<PORT>:<EXPORTER.YAML> -jar <YOUR_APPLICATION.JAR>
```

 **Notes**

- `<PORT>` is required
- `[HOSTNAME]` is optional
  - if provided, must be separated from `<PORT>` using a colon (`:`) (e.g., `server:12345`)

### Concrete Example

```shell
java -javaagent:jmx_prometheus_javaagent-1.3.0.jar=12345:exporter.yaml -jar <YOUR_APPLICATION.JAR>
```

# Basic YAML Configuration

Your application **must** expose RMI.

**exporter.yaml**

```yaml
hostPort: <APPLICATION_HOSTNAME_OR_IP>:<APPLICATION_RMI_PORT>
rules:
- pattern: ".*"
```

... or ...

```yaml
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
2. Run the Standalone JMX Exporter application.
3. Access HTTP mode metrics using a browser to view your metrics.

```
http://<STANDALONE_JMX_EXPORTER_HOSTNAME>:<PORT>/metrics
```

```
# HELP my_count_total example counter
# TYPE my_count_total counter
my_count_total{status="error"} 1.0
my_count_total{status="ok"} 2.0
```

#  Complex YAML Configuration Examples

 Integration tests  provide complex/concrete examples of application and YAML configuration files.

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
