---
title: Standalone Exporter
---

The standalone exporter runs as a separate process and connects to a target JVM over remote JMX/RMI. Prefer the [Java Agent](java-agent) unless you must scrape a remote JVM.

## Artifact

Download `jmx_prometheus_standalone-1.2.0.jar` from the [1.2.0 release](https://github.com/prometheus/jmx_exporter/releases/download/1.2.0/jmx_prometheus_standalone-1.2.0.jar).

## Argument formats

The standalone `main` method parses command-line arguments:

| Format | Behavior |
| --- | --- |
| `<PORT> <EXPORTER_YAML>` | Enable HTTP on `0.0.0.0:<PORT>`. |
| `<HOST>:<PORT> <EXPORTER_YAML>` | Enable HTTP on the specified host and port. |
| `<EXPORTER_YAML>` | Do not start HTTP; useful for OpenTelemetry-only configuration. |

Ports must be from `1` through `65535`.

## HTTP mode example

```bash
java -jar jmx_prometheus_standalone-1.2.0.jar 9404 exporter.yaml
```

```yaml
hostPort: application.example.com:9999
rules:
- pattern: ".*"
```

Equivalent explicit JMX URL:

```yaml
jmxUrl: service:jmx:rmi:///jndi/rmi://application.example.com:9999/jmxrmi
rules:
- pattern: ".*"
```

`hostPort` and `jmxUrl` are mutually exclusive.

## Remote JMX authentication

Use top-level `username` and `password` for remote JMX credentials.

```yaml
hostPort: application.example.com:9999
username: jmx-user
password: jmx-password
rules:
- pattern: ".*"
```

## Remote JMX SSL

If the remote JMX connection requires SSL, enable collector SSL with a boolean value:

```yaml
hostPort: application.example.com:9999
ssl: true
rules:
- pattern: ".*"
```

## OpenTelemetry-only example

```bash
java -jar jmx_prometheus_standalone-1.2.0.jar exporter.yaml
```

```yaml
hostPort: application.example.com:9999
openTelemetry:
  endpoint: http://localhost:4317
rules:
- pattern: ".*"
```

## Lifecycle and errors

Standalone startup occurs through the jar `main` method. Startup creates the collector, starts the enabled exporters, and registers shutdown hooks. Malformed arguments, invalid ports, unreadable YAML, invalid configuration, or remote JMX connection failures fail startup.
