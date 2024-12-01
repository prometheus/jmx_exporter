JMX Exporter Java agent
---

The JMX Exporter Java agent runs in your application as a Java agent.

## Jar

- jmx_prometheus_javaagent-\<VERSION>.jar

## Installation

Example:

```shell
java -javaagent:jmx_prometheus_javaagent-<VERSION>.jar=[HOSTNAME:]<PORT>:<EXPORTER.YAML> -jar <YOUR_APPLICATION.JAR>
```

Description:

- The JMX Exporter Java agent runs as part of your application
- `<PORT>` is required
- `<EXPORTER.YAML>` is required
- `<HOSTNAME>` is optional
  - if included, a colon (`:`) must be used to separate `<HOSTNAME>` and `<PORT>`

## Collection Modes

The JMX Exporter Java agent supports three modes of collection.

- HTTP Mode
- OpenTelemetry Mode
- Combined Mode (HTTP Mode and OpenTelemetry Mode)

### HTTP Mode

HTTP mode exposes metrics on an HTTP endpoint.

- "pull" model

### OpenTelemetry Mode

OpenTelemetry mode pushes metrics to an OpenTelemetry endpoint.

- "push" model

### Combined mode

Both HTTP Mode and OpenTelemetry Mode are enabled.

### HTTP Mode Configuration

<details>
<summary>Click to expand!</summary>

HTTP Mode Configuration TBD

</details>

### OpenTelemetry Mode Configuration

<details>
<summary>Click to expand!</summary>

OpenTelemetry Mode Configuration TBD

</details>

