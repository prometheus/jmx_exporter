---
title: Examples
---

Use source-backed examples when building production configurations.

## Repository examples

Community-provided example YAML files are available in [`examples/`](https://github.com/prometheus/jmx_exporter/tree/1.1.0/examples).

## Integration-test examples

Concrete integration-test-backed configurations are available in [`integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test`](https://github.com/prometheus/jmx_exporter/tree/1.1.0/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test).

## Minimal examples

Java agent HTTP mode:

```yaml
rules:
- pattern: ".*"
```

Standalone HTTP mode:

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
