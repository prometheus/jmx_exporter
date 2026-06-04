---
title: Collector API
---

The collector module is published to Maven Central and is used by JMX Exporter components.

## Public constructors

JMX Exporter 1.2.0 exposes constructors for loading YAML configuration from a file, string, or input stream. The file constructor can also specify collector mode.

## Collector mode

The collector validates mode-specific configuration:

- Java agent mode monitors the local JVM and must not configure `hostPort` or `jmxUrl`.
- Standalone mode connects over remote JMX/RMI and must configure `hostPort` or `jmxUrl`.

## Support statement

The collector module is primarily designed for use within JMX Exporter. Package structure, class names, method names, and method signatures may change based on project needs.
