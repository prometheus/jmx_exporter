JMX Exporter
---

The JMX Exporter is a collector to capture JMX MBean values running as either a Java agent or a standalone application.

## Java agent JMX Exporter

Getting started with the JMX Exporter Java agent...

- [Java agent](java_agent/README.md)

**Notes**

- **Strongly encouraged**

## Standalone JMX Exporter

Getting started with the Standalone JMX Exporter...

- [Standalone](standalone/README.md)

**Notes**

- **The Standalone JMX Exporter has various disadvantages, such as being harder to configure and being unable to expose process metrics (e.g., memory and CPU usage)**


- **In particular, all the `jvm_*` metrics like `jvm_classes_loaded_total`, `jvm_threads_current`,`jvm_threads_daemon` and `jvm_memory_bytes_used` won't be available when using the Standalone JMX Exporter**