---
title: Building / Testing
---

## Build and unit test

Run formatting before Maven validation:

```bash
./mvnw spotless:apply
./mvnw test
```

Integration tests require Docker.

## Full validation

Full Maven validation requires Docker for integration tests:

```bash
./mvnw spotless:apply
./mvnw clean verify
```

## Integration tests

Pull smoke-test images and run the smoke test:

```bash
cd integration_test_suite && ./pull-smoke-test-docker-images.sh
cd ..
./smoke-test.sh
```

## Test scripts

- `./smoke-test.sh` runs a smoke-test integration scenario.
- `./mvnw clean verify` runs full Maven validation when Docker is available.
