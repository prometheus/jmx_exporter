JMX Exporter
---

The JMX Exporter is collector to capture JMX MBean values running as either a Java agent or a standalone application.

---

## Binaries

The JMX Exporter has two binaries.

Java agent

- `jmx_prometheus_javaagent-<VERSION>.jar`

Standalone

- `jmx_prometheus_standalone-<VERSION>.jar`

### Java Agent JMX Exporter

Runs the JMX Exporter as Java agent within an application to collect JMX metrics

- `jmx_prometheus_javaagent-<VERSION>.jar`
- [Java agent](JAVA_AGENT.md) documentation

**Notes**

- **Running the exporter as a Java agent is strongly encouraged**

### Standalone JMX Exporter

Runs the JMX Exporter as a Java application that connects to another Java application via RMI to collect JMX metrics

- `jmx_prometheus_standalone-<VERSION>.jar`
- [Standalone](STANDALONE.md) documentation

**The standalone JMX Exporter has various disadvantages, such as being harder to configure and being unable to expose
process metrics (e.g., memory and CPU usage).**

**In particular, all the `jvm_*` metrics like `jvm_classes_loaded_total`, `jvm_threads_current`,
`jvm_threads_daemon` and `jvm_memory_bytes_used` won't be available when
using the standalone JMX Exporter.**

---

## Modes

The Java agent JMX Exporter and the standalone JMX Exporter both support metrics collection HTTP(S), OpenTelemetry, or the both depending on configuration.

### HTTP(S) Mode

- provides an HTTP(S) endpoint to scrape metrics
- metrics are returned when accessing the HTTP(S) endpoint
- "pull" model

### OpenTelemetry Mode

- periodically collects metrics and pushes them to an OpenTelemetry endpoint
- "push" model

### Combined Mode ... HTTPS(S) and OpenTelemetry

- HTTP(S)
  - provides an HTTP(S) endpoint to scrape metrics
  - metrics are retrieved when accessing the HTTP(S) endpoint 
  - "pull" model


- OpenTelemetry
  - periodically collects metrics and pushes them to an OpenTelemetry endpoint
  - "push" model

**Notes**

- The OpenTelemetry collection is performed periodically. This can result in inconsistent metrics being returned by the HTTP(S) endpoint collection and OpenTelemetry collection due to timing differences.

---

## Common Configuration (Rules)

Common configuration for both binaries.

- [Common Configuration](COMMON_CONFIGURATION.md)