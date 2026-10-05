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

The default `./mvnw clean verify` runs the quick Docker container set. `./run-quick-test.sh` wraps
the same quick container set with a pre-pull and parallel test execution:

```bash
cd integration_test_suite && ./pull-quick-test-docker-images.sh
cd ..
./run-quick-test.sh
```

Run the smoke container set (multiple Java images):

```bash
./run-smoke-test.sh
```

Run the integration tests against the full Docker image set when Docker is available:

```bash
./mvnw spotless:apply
JAVA_DOCKER_IMAGES=ALL PROMETHEUS_DOCKER_IMAGES=ALL ./mvnw test -pl integration_test_suite/integration_tests
```

## Test scripts

- `./run-quick-test.sh` runs the quick integration test container set.
- `./run-smoke-test.sh` runs the smoke integration test container set.
- `./run-stress-test.sh <iterations>` repeats tests to check for flakes.
