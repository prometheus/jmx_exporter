---
title: Building / Testing
---

## Build and unit test

Run formatting before Maven validation:

```bash
./mvnw spotless:apply
./mvnw test -pl '!integration_test_suite'
```

The `integration_test_suite/` module requires Docker. Skip it with `-pl '!integration_test_suite'` when Docker is unavailable.

## Full validation

Full Maven validation requires Docker for integration tests:

```bash
./mvnw spotless:apply
./mvnw clean verify
```

## Integration tests

Pull smoke-test images and run the quick smoke test:

```bash
cd integration_test_suite && ./pull-smoke-test-docker-images.sh
cd ..
./run-quick-test.sh
```

Run all integration tests when Docker is available:

```bash
./mvnw spotless:apply
./mvnw test -pl integration_test_suite/integration_tests
```

## Test scripts

- `./run-quick-test.sh` runs a small integration smoke test set.
- `./run-stress-test.sh <iterations>` repeats tests to check for flakes.
