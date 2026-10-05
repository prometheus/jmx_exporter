---
title: Contributing
---

Contributions are welcome. Start with the repository [CONTRIBUTING.md](https://github.com/prometheus/jmx_exporter/blob/main/CONTRIBUTING.md).

## Formatting and validation

Run Spotless before Maven validation commands:

```bash
./mvnw spotless:apply
```

For unit tests without Docker:

```bash
./mvnw test -pl '!integration_test_suite'
```

Integration tests require Docker.

## Documentation

Documentation should match source behavior. Validate website changes with:

```bash
./scripts/build-documentation.sh
```
