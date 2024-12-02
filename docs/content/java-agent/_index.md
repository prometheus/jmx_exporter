---
title: Java Agent
weight: 1
---

The JMX Exporter Java agent jar provides access to JMX metrics running as a Java agent within your application.

**Use of the JMX Exporter Java agent is strongly encouraged due to the complex application RMI configuration required when running the Standalone JMX Exporter.**

# Jar File

The JMX Exporter Java agent is packaged in `jmx_prometheus_javaagent-<VERSION>.jar`

# Installation

Installation depends on which modes you want to support:

- [HTTP Mode](/java-agent/http-mode/)
- [OpenTelemetry Mode](/java-agent/opentelemetry-mode/)
- [Combined Mode](/java-agent/combined-mode/)