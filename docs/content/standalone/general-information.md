---
title: General Information
weight: 0
---

The Standalone JMX Exporter jar runs as a separate application that connects to your application using RMI and collects metrics.

**Use of the JMX Exporter Java agent is strongly encouraged due to the complex application RMI configuration required when running the Standalone JMX Exporter.**

# Jar File

The Standalone JMX Exporter is packaged in `jmx_prometheus_standalone-<VERSION>.jar`

# Installation

Installation depends on which modes you want to support:

- [HTTP Mode](/java-agent/http_mode/)
- [OpenTelemetry Mode](/java-agent/opentelementry_mode/)
- [Combined Mode](/java-agent/combined_mode/)
