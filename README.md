<picture>
  <img src="https://circleci.com/gh/prometheus/jmx_exporter.svg?style=shield"/>
</picture>

# Prometheus JMX Exporter

The Prometheus JMX Exporter is a collector that exposes Java MBeans of a JMX target in either Prometheus format or OpenMetrics format.

# Releases

Releases are available on [GitHub](https://github.com/prometheus/jmx_exporter/releases) as well as Maven Central.

Release branches (`release-<VERSION>`) are created to allow for versioned documentation.

# Documentation

**Documentation is specific to a release.**

For the current branch, reference the [Manual](MANUAL.md).

For a specific release, reference the `release-<VERSION>` branch for relevant documentation.

Recent releases:

- [0.19.0](https://github.com/prometheus/jmx_exporter/tree/release-0.19.0)
- [0.18.0](https://github.com/prometheus/jmx_exporter/tree/release-0.18.0)
- [0.17.2](https://github.com/prometheus/jmx_exporter/tree/release-0.17.2)

# Issues

For issues (bugs, enhancement requests) use GitHub [Issues](https://github.com/prometheus/jmx_exporter/issues).

For General questions / help see [Getting Help](#getting-help)

# Getting Help

### Google Groups

Community-maintained mailing list [Prometheus Users](https://groups.google.com/g/prometheus-users)  

### Slack

Community-maintained Slack channel `#prometheus-java` on CNCF [Slack](https://slack.cncf.io/).

# Building

Please reference the [Manual](MANUAL.md) for building / testing instructions.

# Contributing

Contributions to the Prometheus JMX Exporter are both welcomed and appreciated.

The project uses a [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow) branching strategy, with release branches for versioned documentation.

Release branch code is considered "locked" - no code changes accepted, but documentation changes are allowed.

**General guidance**

- The `main` branch contains the latest unreleased code
- Release branches `release-<VERSION>` contain code and documentation for a specific release
- New code / changed code should use 4 spaces for indentation
- Don't completely reformat code (makes it hard to review a pull request)
- Expand all Java imports

- Tags are used for releases

**Bug fixes / enhancements**

- Fork the repository
- Create a branch for your work off of `main`
- Make changes on your branch
- Build and test your changes
- Open a pull request targeting `main`, tagging (`@...`) maintainers for review
- A [Developer Certificate of Origin](DCO.md) (DCO) is required for all contributions

**Documentation changes**

- Fork the repository
- Create a branch for your work
- Make changes on your branch
- Build and test your changes
- Open a pull request, tagging (`@...`) maintainers for review
- A [Developer Certificate of Origin](DCO.md) (DCO) is required for all contributions
