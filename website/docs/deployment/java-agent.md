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
| `graceful:<PORT>:<EXPORTER_YAML>` | Enable HTTP on `0.0.0.0:<PORT>`. On startup error, log and clean up without exiting the JVM. |
| `graceful:<HOST>:<PORT>:<EXPORTER_YAML>` | Enable HTTP on the specified host and port. On startup error, log and clean up without exiting the JVM. |
| `graceful:<EXPORTER_YAML>` | Do not start HTTP. On startup error, log and clean up without exiting the JVM. |

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

## Graceful error handling

By default, the Java agent calls `System.exit(1)` when it fails to start, which terminates the JVM. Prefix the agent argument with `graceful:` to disable this behavior for startup errors: the error is still logged and any started resources are closed, but the JVM keeps running.

```bash
java -javaagent:jmx_prometheus_javaagent-1.6.0.jar=graceful:9404:exporter.yaml -jar your-application.jar
```

This option only affects startup errors in the Java agent. Scrape and collection errors are unaffected and continue to be reported via the `jmx_scrape_error` metric without terminating the JVM.
