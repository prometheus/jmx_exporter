<picture>
  <img src="https://circleci.com/gh/prometheus/jmx_exporter.svg?style=shield"/>
</picture>

JMX Exporter
=====

JMX to Prometheus exporter: a collector that can configurable scrape and
expose mBeans of a JMX target.

This exporter is intended to be run as a Java Agent, exposing a HTTP server
and serving metrics of the local JVM. It can be also run as a standalone
HTTP server and scrape remote JMX targets, but this has various
disadvantages, such as being harder to configure and being unable to expose
process metrics (e.g., memory and CPU usage).

**Running the exporter as a Java agent is strongly encouraged.**

# Documentation

**Documentation is specific to a release.**

0.20.0 - [README.md](docs/0.20.0/README/md)
