Integration Test Suite
---

Integration tests use [Paramixel](https://www.paramixel.org).

### Java Docker images tested

- [Smoke test Docker images](https://github.com/prometheus/jmx_exporter/blob/main/integration_test_suite/integration_tests/src/test/resources/smoke-test-java-docker-images.txt)
- [All Docker images](https://github.com/prometheus/jmx_exporter/blob/main/integration_test_suite/integration_tests/src/test/resources/java-docker-images.txt)

### Testing via Maven

Running the integration test suite (smoke test Docker images):

```
./mvnw clean verify
```

Running the integration test suite (all Docker images):

```shell
export JAVA_DOCKER_IMAGES=ALL
./mvnw clean verify
```

Running the integration test suite on a specific Docker image:

```shell
export JAVA_DOCKER_IMAGES="<your custom Docker image>"
./mvnw clean verify
```

Example:

```shell
export JAVA_DOCKER_IMAGES="azul/zulu-openjdk:17"
./mvnw clean verify
```

### Testing via the IDE

#### Running integration tests

Each test package contains a `__ConsolePackageRunner__` class with a `main()` method. Running a `__ConsolePackageRunner__` from an IDE executes all tests in that package.

#### Running local (non-Docker) tests

`LocalTest.java` is a non-Docker test that starts a JMX Exporter locally and verifies functionality with multiple simultaneous HTTP clients. It can be run via `LocalTest.main()` from an IDE.

### Pulling Docker images

Pulling Docker images (not required, but you may see request timeouts/pull failures during testing.)

Smoke test Docker images

```shell
./integration_test_suite/pull-smoke-test-docker-images.sh
```

All Docker images

```shell
./integration_test_suite/pull-docker-images.sh
```

## Notes

- You may need to set up Docker hub login to pull images
- Testing all Docker images requires a significant amount of time (hours)
- For development, use `./run-quick-test.sh` for a faster test cycle

---

### Docker Configuration changes

When running the integration test suite, Docker may need to be configured to support a large number of network addresses.

On Linux:

```shell
/etc/docker/daemon.json
```

```yaml
{
  "default-address-pools" : [
    {
      "base" : "172.16.0.0/16",
      "size" : 24
    },
    {
      "base" : "192.168.0.0/16",
      "size" : 24
    }
  ]
}
```