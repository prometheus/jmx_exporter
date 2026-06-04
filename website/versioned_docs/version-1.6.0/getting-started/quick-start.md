---
title: Quick Start
---

Use the Java agent when possible. It runs inside the target JVM and avoids remote JMX/RMI configuration.

## Download the Java agent

Download `jmx_prometheus_javaagent-1.6.0.jar` from the [1.6.0 release](https://github.com/prometheus/jmx_exporter/releases/download/1.6.0/jmx_prometheus_javaagent-1.6.0.jar).

## Create `exporter.yaml`

```yaml
rules:
- pattern: ".*"
```

This minimal configuration collects matching MBean attributes using the default metric format.

## Start your application

```bash
java -javaagent:jmx_prometheus_javaagent-1.6.0.jar=9404:exporter.yaml -jar your-application.jar
```

When no host is specified, the HTTP server binds to `0.0.0.0`. The example exposes metrics on port `9404`.

## Verify metrics

Open:

```text
http://localhost:9404/metrics
```

Example output:

```prometheus
# HELP jmx_config_reload_failure_total Number of times configuration have failed to be reloaded.
# TYPE jmx_config_reload_failure_total counter
jmx_config_reload_failure_total 0.0
```

## Next steps

- Learn the [deployment modes](../deployment/modes).
- Configure [rules](../configuration/rules).
- Review the complete [configuration reference](../reference/configuration).
