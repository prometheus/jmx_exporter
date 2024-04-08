<picture>
  <img src="https://circleci.com/gh/prometheus/jmx_exporter.svg?style=shield"/>
</picture>

# JMX Exporter

[![Build Status](https://circleci.com/gh/prometheus/jmx_exporter.svg?style=svg)](https://circleci.com/gh/prometheus/jmx_exporter)

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

0.20.0 - [README.md](docs/0.20.0/README.md)

# Contributing and community

See [CONTRIBUTING.md](CONTRIBUTING.md) and the [community section](http://prometheus.io/community/) of the Prometheus homepage.

The Prometheus Java community is present on the [CNCF Slack](https://cloud-native.slack.com) on `#prometheus-java`, and we have a fortnightly community call in the [Prometheus public calendar](https://prometheus.io/community/).

# License

Apache License 2.0, see [LICENSE](LICENSE).
