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

Your application **must** expose RMI.

**exporter.yaml**

```yaml
hostPort: <APPLICATION_HOSTNAME>:<APPLICATION_RMI_PORT>
rules:
- pattern: ".*"
```

... or ...

```yaml
jmxUrl: service:jmx:rmi:///jndi/rmi://<APPLICATION_HOSTNAME>:<APPLICATION_RMI_PORT>/jmxrmi
rules:
- pattern: ".*"
```

### Additional RMI Configuration

#### RMI SSL

If your application's RMI server requires SSL you can add `ssl: true`

```yaml
hostPort: <APPLICATION_HOSTNAME>:<APPLICATION_RMI_PORT>
ssl: true
rules:
- pattern: ".*"
```

#### RMI Username / Password

If your application's RMI server requires authentication, you can add `username` and `password`

```yaml
hostPort: <APPLICATION_HOSTNAME>:<APPLICATION_RMI_PORT>
username: <APPLICATION_RMI_USERNAME>
password: <APPLICATION_RMI_PASSWORD>
rules:
- pattern: ".*"
```

# Advanced YAML Configuration

Reference HTTP Mode [Rules](/rules/rules/) for various `exporter.yaml` metrics configuration options.

# Metrics

1. Run your application.
2. Run the Standalone JMX Exporter application.
3. Access HTTP Mode metrics using a browser to view your metrics.

```
http://<STANDALONE_JMX_EXPORTER_HOSTNAME>:<PORT>/metrics
```

```
# HELP my_count_total example counter
# TYPE my_count_total counter
my_count_total{status="error"} 1.0
my_count_total{status="ok"} 2.0
```