---
title: Isolator Java Agent
---

The Isolator Java Agent starts one or more JMX Exporter Java agent jars in isolated classloaders inside the same JVM.

## Artifacts

Download both jars from the [1.3.0 release](https://github.com/prometheus/jmx_exporter/releases/tag/1.3.0):

- `jmx_prometheus_isolator_javaagent-1.3.0.jar`
- `jmx_prometheus_javaagent-1.3.0.jar`

## Argument format

```text
-javaagent:jmx_prometheus_isolator_javaagent-1.3.0.jar=<EXPORTER_JAVA_AGENT_JAR>=<JAVA_AGENT_ARGUMENT>[,<EXPORTER_JAVA_AGENT_JAR>=<JAVA_AGENT_ARGUMENT>]
```

Each nested Java agent argument uses the standard [Java Agent](java-agent) argument format.

## Example

```bash
java -javaagent:jmx_prometheus_isolator_javaagent-1.3.0.jar=jmx_prometheus_javaagent-1.3.0.jar=8080:exporter.yaml,jmx_prometheus_javaagent-1.3.0.jar=8081:exporter2.yaml -jar your-application.jar
```

## Lifecycle and errors

The isolator agent loads each configured exporter jar in its own classloader and starts it during JVM startup. Invalid nested agent arguments, unreadable jars, unreadable YAML, or invalid exporter configuration fail startup.
