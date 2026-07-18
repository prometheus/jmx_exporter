---
title: Isolator Java Agent
---

The isolator Java agent starts one or more JMX Exporter Java agent jars in isolated classloaders. Use it only when you need multiple independent exporter instances in the same JVM.

## Artifacts

Download both jars from the 1.6.0 release:

- [`jmx_prometheus_isolator_javaagent-1.6.0.jar`](https://github.com/prometheus/jmx_exporter/releases/download/1.6.0/jmx_prometheus_isolator_javaagent-1.6.0.jar)
- [`jmx_prometheus_javaagent-1.6.0.jar`](https://github.com/prometheus/jmx_exporter/releases/download/1.6.0/jmx_prometheus_javaagent-1.6.0.jar)

## Argument format

```text
-javaagent:jmx_prometheus_isolator_javaagent-1.6.0.jar=/path/to/exporter1.jar=agentArgs1,/path/to/exporter2.jar=agentArgs2
```

Each comma-separated item contains the exporter jar path, an equals sign, and the Java agent arguments for that exporter.

## Example

```bash
java -javaagent:jmx_prometheus_isolator_javaagent-1.6.0.jar=jmx_prometheus_javaagent-1.6.0.jar=9404:exporter.yaml,jmx_prometheus_javaagent-1.6.0.jar=9405:exporter2.yaml -jar your-application.jar
```

## Lifecycle and errors

The isolator agent starts each exporter through an isolated classloader on a daemon startup thread. Startup waits for the implementation timeout and logs a warning if the thread is still alive. A missing, empty, or malformed isolator argument fails startup.

## Logging backend

The isolator and nested JMX Exporter agents use JUL-independent native logging by default. Native
logging writes `INFO`, `WARN`, and `ERROR` messages to standard error. Set
`-Djmx.prometheus.exporter.logging.backend=jul` or
`JMX_PROMETHEUS_EXPORTER_LOGGING_BACKEND=jul` to use Java Util Logging instead. The system property
takes precedence, and the selected backend is fixed when each logger is first created. JUL is
process-wide and may initialize the application's configured `LogManager` during agent startup.
