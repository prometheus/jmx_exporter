---
title: Collector API
---

The collector module provides the public collector API for embedding JMX collection in Java applications. Preserve public API and exception semantics when depending on it.

## Public constructors

```java
public JmxCollector(File in) throws IOException, MalformedObjectNameException
public JmxCollector(File in, JmxCollector.Mode mode) throws IOException, MalformedObjectNameException
public JmxCollector(String yamlConfig) throws MalformedObjectNameException
public JmxCollector(InputStream inputStream) throws MalformedObjectNameException
```

The file and stream constructors load exporter YAML. The string constructor loads YAML from a string.

## Collector mode

```java
public enum JmxCollector.Mode {
    AGENT,
    STANDALONE
}
```

Mode controls internal collection behavior for agent and standalone deployments.

## Support statement

The collector library is the core module used by the Java agent and standalone exporter. Public API behavior should be treated as stable for a given release line unless a breaking change is explicitly documented.
