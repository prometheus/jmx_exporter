# Integration Test Suite

### Smoke test Java Docker images tested

[Smoke test Docker images](https://github.com/prometheus/jmx_exporter/blob/main/integration_test_suite/integration_tests/src/test/resources/smoke-test-java-docker-images.txt)

### All Java Docker images tested (all)

[All Docker images](https://github.com/prometheus/jmx_exporter/blob/main/integration_test_suite/integration_tests/src/test/resources/java-docker-images.txt)

### Running the integration test suite (smoke test Docker images)

```
./mvnw clean verify
```

### Run the integration test suite (all Docker images)

```shell
export JAVA_DOCKER_IMAGES=ALL
./mvnw clean verify
```

### Run the integration test suite on a specific Docker image

```shell
export JAVA_DOCKER_IMAGES="<your custom Docker image>"
./mvnw clean verify
```

Example:

```shell
export JAVA_DOCKER_IMAGES="azul/zulu-openjdk:17"
./mvnw clean verify
```

### Pulling Docker images

Pulling Docker images (not required, but you may see request timeouts/pull failures during testing.)

Smoke test Docker images

```shell
./integration_test_suite/pull-smoke-test-java-docker-images.sh
```

All Docker images

```shell
./integration_test_suite/pull-java-docker-images.sh
```

## Notes

- You may need to set up Docker hub login to pull images

# Docker networks

By default, Docker is not configured to run a large number images simultaneously.

You can increase Docker networks by creating a Docker network configuration file...

```
/etc/docker/daemon.json
```

... with the content...

```yaml
{
  "default-address-pools": [
    {
      "base": "172.16.0.0/16",
      "size": 24
    },
    {
      "base": "192.168.0.0/16",
      "size": 24
    }
  ]
}
```

## Notes

- Docker will need to be restarted