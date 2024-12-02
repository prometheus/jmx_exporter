---
title: Standalone
weight: 2
---

The Standalone JMX Exporter jar runs as a separate application that connects to your application using RMI and collects metrics.

**Use of the JMX Exporter Java agent is strongly encouraged due to the complex application RMI configuration required when running the Standalone JMX Exporter.**

# Jar File

The Standalone JMX Exporter is packaged in `jmx_prometheus_standalone-<VERSION>.jar`

# Installation

Installation depends on which modes you want to support:

- [HTTP mode](/java-agent/http-mode/)
- [OpenTelemetry mode](/java-agent/opentelemetry-mode/)
- [Combined mode](/java-agent/combined-mode/)