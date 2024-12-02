---
title: "JMX Exporter"
weight: 1
---

The JMX Exporter is a collector to capture JMX MBean values.

### Java Agent

The JMX Exporter Java agent runs as a Java agent within your application and collects JMX MBean values.

**Use of the JMX Exporter Java agent is strongly encouraged due to the complex application RMI configuration required when running the Standalone JMX Exporter.**

### Standalone

The Standalone JMX Exporter runs as a separate application that connects to your application using RMI and collects JMX MBean values.
