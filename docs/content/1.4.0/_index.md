---
title: "JMX Exporter 1.4.0"
weight: 996
geekdocCollapseSection: true
---

This is the documentation for the [Prometheus JMX Exporter](https://github.com/prometheus/jmx_exporter) version 1.4.0.

The main new features of the 1.4.0 release are:

* **Disable JVM Metrics**: Support for completely disabling JVM metrics when using the JMX Exporter Java agent.


* **Basic authentication Environment Variable Support**: Support for using environment variable to inject Basic authentication password.


* **HTTP SSL Environment Variable Support**: Support for using environment variables for HTTP server SSL keystore and truststore passwords.


**Documentation and Examples**

Community provided example YAML configuration files:

- [examples](https://github.com/prometheus/jmx_exporter/tree/main/examples)

Integration tests also provide complex/concrete examples of application and YAML configuration files:

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
