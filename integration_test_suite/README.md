# Integration test suite

### Smoke test Docker images tested

```
eclipse-temurin:8-alpine
eclipse-temurin:11-alpine
eclipse-temurin:17-alpine
eclipse-temurin:19-alpine
eclipse-temurin:20-alpine
```

By default, integration tests only run using these Docker images.

### Docker images tested (all)

[All Docker images](https://github.com/prometheus/jmx_exporter/blob/main/integration_test_suite/integration_tests/src/test/resources/docker-image-names.all.txt)

### Running the integration test suite (smoke test Docker images)

```
./mvnw clean verify
```

### Run the integration test suite (all Docker images)

```shell
export DOCKER_IMAGE_NAMES=ALL
./mvnw clean verify
```

### Run the integration test suite on a specific Docker image

```shell
export DOCKER_IMAGE_NAMES="<your custom Docker image>"
./mvnw clean verify
```

- `java` is expected to be in the user's `PATH`

### Pulling Docker images

Pulling Docker images (not required, but you may see request timeouts/pull failures during testing.)

Smoke test Docker images

```shell
./integration_test_suite/docker-pull-images.smoke-test.sh
```

All Docker images

```shell
./integration_test_suite/docker-pull-images.all.sh
```

## Notes

- You may need to set up Docker hub login to pull images
