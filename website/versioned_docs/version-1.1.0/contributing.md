---
title: Contributing
---

Contributions are welcome. Start with the repository [CONTRIBUTING.md](https://github.com/prometheus/jmx_exporter/blob/1.1.0/CONTRIBUTING.md).

## Formatting and validation

Run Spotless before Maven validation commands:

```bash
./mvnw spotless:apply
```

For Maven validation:

```bash
./mvnw test
```

Integration tests require Docker.

## Documentation

Documentation should match source behavior for the documented release.
