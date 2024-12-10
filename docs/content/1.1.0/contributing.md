---
title: "Contributing"
weight: 5
---

The JMX Exporter uses GitHub to manage issues and reviews of pull requests.

### Trivial Fixes / Improvements

If you have a trivial fix or improvement, go ahead and create a pull request, tagging the maintainers.

- [MAINTAINERS.md](https://github.com/prometheus/jmx_exporter/blob/main/MAINTAINERS.md)

### Advanced Fixes / Improvements

If you plan to do something more involved, first discuss your ideas on the [mailing list](https://groups.google.com/forum/?fromgroups#!forum/prometheus-developers).

This will avoid unnecessary work and surely give you and us a good deal of inspiration.

### Integration Tests

Integration tests use [Verifyica](https://github.com/verifyica-team/verifyica) for integration testing and are required for code changes.

### Code Formatting

Code formatting is enforced using the [Maven Spotless Plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven) along with [Google Java Format](https://github.com/google/google-java-format) as part of the build.

### Branching Strategy

[GitHub Flow](https://docs.github.com/en/get-started/using-github/github-flow) is used for branching.

- Pull requests should be opened against `main` for the next release.

