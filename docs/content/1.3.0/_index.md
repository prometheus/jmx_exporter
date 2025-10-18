---
title: "JMX Exporter 1.3.0"
weight: 997
geekdocCollapseSection: true
---

This is the documentation for the [Prometheus JMX Exporter](https://github.com/prometheus/jmx_exporter) version 1.3.0.

The main new features of the 1.3.0 release are:

* **Isolator Java Agent**: A new Java agent that allows running multiple isolated and independent JMX Exporter instances in the same JVM.


* **Mutual TLS Authentication**: Support for mutual TLS authentication HTTP server authentication.


**Documentation and Examples**

Community provided example YAML configuration files:

- [examples](https://github.com/prometheus/jmx_exporter/tree/main/examples)

Integration tests also provide complex/concrete examples of application and YAML configuration files:

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
