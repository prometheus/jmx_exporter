---
title: Isolator Java Agent (BETA)
weight: 2
geekdocCollapseSection: true
---

The JMX Exporter Isolator Java agent allows running multiple isolated and independent JMX Exporter Java agents.

Each isolated JMX Exporter Java agent will have its own configuration (port, rules, etc.)

# Jar Files

The Isolator Java agent jar file is published via GitHub Releases.

- [jmx_prometheus_isolator_javaagent-1.3.0.jar](https://github.com/prometheus/jmx_exporter/releases/download/1.3.0/jmx_prometheus_isolator_javaagent-1.3.0.jar)

The Isolator Java agent requires a Prometheus JMX Exporter Java agent jar.

- [jmx_prometheus_javaagent-1.3.0.jar](https://github.com/prometheus/jmx_exporter/releases/download/1.3.0/jmx_prometheus_javaagent-1.3.0.jar)

# Installation

```bash
java -javaagent:jmx_prometheus_isolator_javaagent-<VERSION>.jar=<EXPORTER_JAVA_AGENT_JAR>=[HOSTNAME:]<PORT>:<EXPORTER.YAML>[,EXPORTER_JAVA_AGENT_JAR>=[HOSTNAME:]<PORT>:<EXPORTER.YAML>] -jar <YOUR_APPLICATION.JAR>
```

# Example

```bash
java -javaagent:jmx_prometheus_isolator_javaagent-1.3.0.jar=jmx_prometheus_javaagent-1.3.0.jar=8080:exporter.yaml,jmx_prometheus_javaagent-1.3.0.jar=8081:exporter2.yaml -jar <YOUR_APPLICATION.JAR>
```
