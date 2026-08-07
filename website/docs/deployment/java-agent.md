---
title: Java Agent
---

The Java agent runs inside the target JVM. It is the recommended deployment mode for most users.

## Artifact

Download `jmx_prometheus_javaagent-1.6.0.jar` from the [1.6.0 release](https://github.com/prometheus/jmx_exporter/releases/download/1.6.0/jmx_prometheus_javaagent-1.6.0.jar).

## Argument formats

The Java agent parses one agent argument string:

| Format | Behavior |
| --- | --- |
| `<PORT>:<EXPORTER_YAML>` | Enable HTTP on `0.0.0.0:<PORT>`. |
| `<HOST>:<PORT>:<EXPORTER_YAML>` | Enable HTTP on the specified host and port. |
| `<EXPORTER_YAML>` | Do not start HTTP; useful for OpenTelemetry-only configuration. |

Ports must be from `1` through `65535`. IPv6 hosts must be enclosed in brackets, for example `[::1]:9404:exporter.yaml`.

## HTTP mode example

```bash
java -javaagent:jmx_prometheus_javaagent-1.6.0.jar=9404:exporter.yaml -jar your-application.jar
```

```yaml
rules:
- pattern: ".*"
```

Metrics are available at `http://localhost:9404/metrics`.

## OpenTelemetry-only example

```bash
java -javaagent:jmx_prometheus_javaagent-1.6.0.jar=exporter.yaml -jar your-application.jar
```

```yaml
openTelemetry:
  endpoint: http://localhost:4317
rules:
- pattern: ".*"
```

## Combined mode example

```bash
java -javaagent:jmx_prometheus_javaagent-1.6.0.jar=9404:exporter.yaml -jar your-application.jar
```

```yaml
openTelemetry:
  endpoint: http://localhost:4317
rules:
- pattern: ".*"
```

## Lifecycle and errors

The agent starts through `premain` at JVM startup or `agentmain` when attached. Startup registers the JMX collector and starts the enabled exporters. Malformed arguments, invalid ports, unreadable YAML, or invalid configuration fail startup.

## Logging backend

The Java agent uses its JUL-independent native logging backend by default so it does not initialize
or configure the application's Java Util Logging (JUL) system during startup. Native logging writes
`INFO`, `WARN`, and `ERROR` messages to standard error.

To integrate exporter logs with JUL instead, set either:

```bash
-Djmx.prometheus.exporter.logging.backend=jul
```

or `JMX_PROMETHEUS_EXPORTER_LOGGING_BACKEND=jul`.

Supported values are `native` and `jul`. The system property takes precedence over the environment
variable. The selected backend is fixed when each logger is first created. JUL is process-wide, so
selecting it may initialize the application's configured `LogManager` during Java agent startup.
