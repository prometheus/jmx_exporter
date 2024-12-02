---
title: Java Agent
weight: 1
---

The JMX Exporter Java agent jar provides access to JMX metrics running as a Java agent within your application.

**Use of the JMX Exporter Java agent is strongly encouraged due to the complex application RMI configuration required when running the Standalone JMX Exporter.**

# Jar File

The JMX Exporter Java agent jar file is published to Maven Central.

- [jmx_prometheus_javaagent-1.1.0.jar](https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/1.1.0/jmx_prometheus_javaagent-1.1.0.jar)

# Installation

Installation depends on which modes you want to support:

- [HTTP mode](/java-agent/http-mode/)
- [OpenTelemetry mode](/java-agent/opentelemetry-mode/)
- [Combined mode](/java-agent/combined-mode/)