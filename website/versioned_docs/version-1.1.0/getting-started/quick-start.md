---
title: Quick Start
---

Use the Java agent when you can run the exporter inside the target JVM.

## Download the Java agent

Download [`jmx_prometheus_javaagent-1.1.0.jar`](https://github.com/prometheus/jmx_exporter/releases/download/1.1.0/jmx_prometheus_javaagent-1.1.0.jar) from the 1.1.0 release.

## Create `exporter.yaml`

```yaml
rules:
- pattern: ".*"
```

## Start the application

```bash
java -javaagent:jmx_prometheus_javaagent-1.1.0.jar=9404:exporter.yaml -jar your-application.jar
```

The Java agent starts an HTTP server on port `9404`. Metrics are available at `http://localhost:9404/metrics`.

## Next steps

- [Java Agent](../deployment/java-agent) explains Java agent argument formats.
- [Rules](../configuration/rules) explains metric rule configuration.
- [Examples](../examples/) links to source-backed example configurations.
