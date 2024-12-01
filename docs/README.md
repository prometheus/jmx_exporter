JMX Exporter
---

The JMX Exporter allows collection of JMX MBean metrics.

The project ships both a Java agent and standalone exporter jars.

- jmx_prometheus_javaagent-\<VERSION>.jar
- jmx_prometheus_standalone-\<VERSION>.jar

## Java agent

The JMX Exporter Java agent runs as part of your application and collects JMX MBean metrics.

### Jar

- jmx_prometheus_javaagent-\<VERSION>.jar

### Documentation

- [Java agent](java_agent/README.md)

**Notes**

- **Strongly encouraged**

## Standalone

The Standalone JMX Exporter runs as a separate application to your application and collects JMX MBean metrics via RMI. 

### Jar

- jmx_prometheus_standalone-\<VERSION>.jar

### Documentation

- [Standalone](standalone/README.md)

**Notes**

- **The Standalone JMX Exporter has various disadvantages, such as being harder to configure and being unable to expose process metrics (e.g., memory and CPU usage)**


- **In particular, all the `jvm_*` metrics like `jvm_classes_loaded_total`, `jvm_threads_current`,`jvm_threads_daemon` and `jvm_memory_bytes_used` won't be available when using the Standalone JMX Exporter**

## Common Configuration

See [Common Configuration](COMMON_CONFIGURATION.md) for details.