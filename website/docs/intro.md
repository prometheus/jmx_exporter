---
title: JMX Exporter 1.6.0
slug: /
---

The Prometheus JMX Exporter collects Java Management Extensions (JMX) MBean values and exports them as Prometheus metrics, or sends them through OpenTelemetry when configured.

## Choose a deployment mode

| Mode | Use when | Notes |
| --- | --- | --- |
| Java agent | You can start or attach an agent inside the target JVM. | Recommended for most users because it avoids remote JMX/RMI setup. |
| Standalone exporter | You must scrape a JVM over remote JMX/RMI. | Requires the target application to expose remote JMX correctly. |
| Isolator Java agent | You need multiple isolated Java agent exporters in one JVM. | Starts each exporter jar in an isolated classloader. |

## Start here

- [Quick start](getting-started/quick-start) gets metrics flowing with the Java agent.
- [Deployment modes](deployment/modes) explains HTTP, OpenTelemetry, and combined operation.
- [Configuration](configuration/) explains the exporter YAML file.
- [Examples](examples/) links to application examples and integration-test-backed configurations.
- [Artifacts](reference/artifacts) lists the 1.6.0 jars and coordinates.

Community examples are available in the repository [examples directory](https://github.com/prometheus/jmx_exporter/tree/main/examples). Integration tests provide concrete source-backed examples under [integration test resources](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test).
