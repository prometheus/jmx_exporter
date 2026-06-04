---
title: Artifacts
---

JMX Exporter 1.1.0 artifacts are published through GitHub Releases.

## Release jars

| Artifact | Link |
| --- | --- |
| Java agent | [`jmx_prometheus_javaagent-1.1.0.jar`](https://github.com/prometheus/jmx_exporter/releases/download/1.1.0/jmx_prometheus_javaagent-1.1.0.jar) |
| Standalone exporter | [`jmx_prometheus_standalone-1.1.0.jar`](https://github.com/prometheus/jmx_exporter/releases/download/1.1.0/jmx_prometheus_standalone-1.1.0.jar) |

## Collector Maven coordinates

The collector module is published as a library:

```xml
<dependency>
  <groupId>io.prometheus.jmx</groupId>
  <artifactId>collector</artifactId>
  <version>1.1.0</version>
</dependency>
```
