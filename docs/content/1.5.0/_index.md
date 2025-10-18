---
title: "JMX Exporter 1.5.0"
weight: 995
geekdocCollapseSection: true
---

This is the documentation for the [Prometheus JMX Exporter](https://github.com/prometheus/jmx_exporter) version 1.5.0.

The main new features of the 1.5.0 release are:

* **HTTP Basic authentication Environment Variable Support:** Support for using environment variables to inject HTTP Basic authentication username/password. (previously just password.)

* **Standalone JMX Authentication Environment Variable Support:** Support for using environment variables to inject JMX username/password.

**Documentation and Examples**

Community provided example YAML configuration files:

- [examples](https://github.com/prometheus/jmx_exporter/tree/main/examples)

Integration tests also provide complex/concrete examples of application and YAML configuration files:

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
