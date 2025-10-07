---
title: Standalone exporter
weight: 3
geekdocCollapseSection: true
---

The Standalone JMX Exporter runs as a separate application that connects to your application using RMI and collects JMX MBean values.

**Use of the JMX Exporter Java agent is strongly encouraged due to the complex application RMI configuration required when running the Standalone JMX Exporter.**

# Jar File

The Standalone JMX Exporter jar file is published via GitHub Releases.

- [jmx_prometheus_standalone-1.5.0.jar](https://github.com/prometheus/jmx_exporter/releases/download/1.5.0/jmx_prometheus_standalone-1.5.0.jar)

# Installation

Installation depends on which modes you want to support:

- [HTTP mode](./http-mode/)
- [OpenTelemetry mode](./opentelemetry-mode/)
- [Combined mode](./combined-mode/)
