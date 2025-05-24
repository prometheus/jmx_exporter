---
title: Miscellaneous
weight: 4
---

HTTP mode supports HTTP server thread pool tuning.

**Notes**

- By default, a maximum of 10 threads are used

### Configuration

```yaml
httpServer:
  threads:
    minimum: 1
    maximum: 10
    keepAliveTime: 120 # seconds
```

- `minimum` - minimum number of threads
- `maximum` - maximum number of threads
- `keepAliveTime` - thread keep-alive time in seconds

**Notes**

- If the work queue is full, the request will be blocked until space is available in the work queue for the request

#  Complex YAML Configuration Examples

Integration tests  provide complex/concrete examples of application and YAML configuration files.

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
