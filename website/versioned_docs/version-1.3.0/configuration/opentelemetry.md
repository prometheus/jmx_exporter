---
title: OpenTelemetry
---

OpenTelemetry mode periodically collects metrics and exports them to an OpenTelemetry endpoint.

## Configuration fields

| Key | Description |
| --- | --- |
| `openTelemetry.endpoint` | Optional endpoint URL. |
| `openTelemetry.protocol` | Optional protocol. |
| `openTelemetry.interval` | Optional positive integer interval in seconds. |
| `openTelemetry.headers` | Optional string map. |
| `openTelemetry.timeoutSeconds` | Optional positive integer timeout in seconds. |
| `openTelemetry.resourceAttributes` | Optional string map. |
| `openTelemetry.serviceInstanceId` | Optional non-blank string. |
| `openTelemetry.serviceNamespace` | Optional non-blank string. |
| `openTelemetry.serviceName` | Optional non-blank string. |
| `openTelemetry.serviceVersion` | Optional non-blank string. |

## OpenTelemetry-only Java agent

```bash
java -javaagent:jmx_prometheus_javaagent-1.3.0.jar=exporter.yaml -jar your-application.jar
```

```yaml
openTelemetry:
  endpoint: http://localhost:4317
  protocol: http/protobuf
  interval: 60
  timeoutSeconds: 10
  resourceAttributes:
    service.namespace: example
  serviceName: example-service
rules:
- pattern: ".*"
```

## OpenTelemetry-only standalone

```bash
java -jar jmx_prometheus_standalone-1.3.0.jar exporter.yaml
```

```yaml
hostPort: application.example.com:9999
openTelemetry:
  endpoint: http://localhost:4317
rules:
- pattern: ".*"
```

## Combined mode

Use an HTTP-enabled command and include `openTelemetry` in the YAML to run both HTTP and OpenTelemetry exporters.
