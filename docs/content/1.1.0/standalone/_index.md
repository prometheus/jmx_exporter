---
title: Standalone
weight: 2
---

The Standalone JMX Exporter runs as a separate application that connects to your application using RMI and collects JMX MBean values.

**Use of the JMX Exporter Java agent is strongly encouraged due to the complex application RMI configuration required when running the Standalone JMX Exporter.**

# Jar File

The Standalone JMX Exporter jar file is published to Maven Central.

- [jmx_prometheus_standalone-1.1.0.jar](https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_standalone/1.1.0/jmx_prometheus_standalone-1.1.0.jar)

# Installation

Installation depends on which modes you want to support:

- [HTTP mode](/java-agent/http-mode/)
- [OpenTelemetry mode](/java-agent/opentelemetry-mode/)
- [Combined mode](/java-agent/combined-mode/)
