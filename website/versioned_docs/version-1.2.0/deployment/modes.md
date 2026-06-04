---
title: Deployment Modes
---

JMX Exporter 1.2.0 can expose metrics over HTTP, export through OpenTelemetry, or run both exporters from one configuration.

## HTTP mode

HTTP mode starts an HTTP server and serves Prometheus metrics. HTTP enablement is controlled by Java agent or standalone command arguments that include a host and port.

- Java agent: `-javaagent:jmx_prometheus_javaagent-1.2.0.jar=9404:exporter.yaml`
- Standalone: `java -jar jmx_prometheus_standalone-1.2.0.jar 9404 exporter.yaml`

The metrics path is `/metrics`.

## OpenTelemetry mode

OpenTelemetry mode is enabled by the `openTelemetry` section in the exporter YAML. Use an argument form without an HTTP port when you want OpenTelemetry-only operation.

## Combined mode

Combined mode starts both HTTP and OpenTelemetry exporters. Use an HTTP-enabled command and include `openTelemetry` in the YAML.

```yaml
openTelemetry:
  endpoint: http://localhost:4317
rules:
- pattern: ".*"
```

See [Java Agent](java-agent), [Standalone Exporter](standalone), and [OpenTelemetry configuration](../configuration/opentelemetry).
