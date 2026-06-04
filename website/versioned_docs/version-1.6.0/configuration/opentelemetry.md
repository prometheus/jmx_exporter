---
title: OpenTelemetry
---

OpenTelemetry export is enabled when the exporter YAML contains an `openTelemetry` section. If the section is absent, no OpenTelemetry exporter is started.

## Configuration fields

| Field | Description |
| --- | --- |
| `endpoint` | Optional URL for the OpenTelemetry endpoint. |
| `protocol` | Optional protocol string passed to the OpenTelemetry exporter. |
| `interval` | Optional export interval in seconds. Must be greater than zero. |
| `timeoutSeconds` | Optional timeout in seconds. Must be greater than zero. |
| `headers` | Optional map of string headers. |
| `resourceAttributes` | Optional map of string resource attributes. |
| `serviceInstanceId` | Optional non-blank service instance id. |
| `serviceNamespace` | Optional non-blank service namespace. |
| `serviceName` | Optional non-blank service name. |
| `serviceVersion` | Optional non-blank service version. |

## OpenTelemetry-only Java agent

```bash
java -javaagent:jmx_prometheus_javaagent-1.6.0.jar=exporter.yaml -jar your-application.jar
```

```yaml
openTelemetry:
  endpoint: http://localhost:4317
  protocol: grpc
  interval: 60
  timeoutSeconds: 30
rules:
- pattern: ".*"
```

## OpenTelemetry-only standalone

```bash
java -jar jmx_prometheus_standalone-1.6.0.jar exporter.yaml
```

```yaml
hostPort: application.example.com:9999
openTelemetry:
  endpoint: http://localhost:4317
rules:
- pattern: ".*"
```

## Combined mode

```bash
java -javaagent:jmx_prometheus_javaagent-1.6.0.jar=9404:exporter.yaml -jar your-application.jar
```

```yaml
openTelemetry:
  endpoint: http://localhost:4317
  protocol: grpc
  headers:
    Authorization: Bearer token
  resourceAttributes:
    service.name: jmx-exporter
  serviceInstanceId: instance-001
  serviceNamespace: prometheus
  serviceName: jmx-exporter
  serviceVersion: "1.6.0"
rules:
- pattern: ".*"
```
