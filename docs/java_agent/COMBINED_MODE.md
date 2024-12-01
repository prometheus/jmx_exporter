JMX Exporter Java agent / Combined Mode
---

Both HTTP mode and OpenTelemetry mode are enabled.

## Jar

- jmx_prometheus_javaagent-\<VERSION>.jar

## Installation

Example:

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=<PORT>:<EXPORTER.YAML> -jar <YOUR_APPLICATION.JAR>
```

Description:

- The JMX Exporter Java agent runs as part of your application
- `<PORT>` is required for HTTP mode
- `<HOSTNAME>` is optional 
- Listens for metrics requests on all IP addresses on port `<PORT>` 

## OpenTelemetry Configuration

OpenTelemetry must be configured in your exporter.yaml file...

### OpenTelemetry specific configuration

```yaml
openTelemetry:
  endpoint: http://prometheus:9090/api/v1/otlp
  protocol: http/protobuf
  interval: 60
```

### Working example

```yaml
openTelemetry:
  endpoint: http://prometheus:9090/api/v1/otlp
  protocol: http/protobuf
  interval: 60
rules:
  - pattern: ".*"
```

## Common Configuration

See [Common Configuration](../COMMON_CONFIGURATION.md) for details.
