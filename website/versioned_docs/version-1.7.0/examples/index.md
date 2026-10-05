---
title: Examples
---

Use source-backed examples when building production configurations.

## Repository examples

Community-provided example YAML files are available in [`examples/`](https://github.com/prometheus/jmx_exporter/tree/main/examples), including examples for Kafka, Cassandra, Tomcat, WildFly, Spark, ZooKeeper, and other applications.

## Integration-test examples

Concrete integration-test-backed configurations are available in [`integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test`](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test). These examples cover Java agent, standalone, authentication, TLS, metrics path, thread pool, and HTTP server scenarios.

## Minimal examples

Java agent HTTP mode:

```yaml
rules:
- pattern: ".*"
```

Standalone HTTP mode against remote JMX:

```yaml
hostPort: application.example.com:9999
rules:
- pattern: ".*"
```

OpenTelemetry:

```yaml
openTelemetry:
  endpoint: http://localhost:4317
rules:
- pattern: ".*"
```
