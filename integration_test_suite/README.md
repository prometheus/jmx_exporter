# Integration test suite

### Smoke test Docker images tested

By default, integration tests only test these containers...

```
azul/zulu-openjdk:8
azul/zulu-openjdk:11
azul/zulu-openjdk:17
azul/zulu-openjdk:19
openjdk:6
ticketfly/java:6
```

### Docker images tested

By changing the `integration_tests/pom.xml` file you can test some or all of these containers...

```
amazoncorretto:8
amazoncorretto:11
amazoncorretto:17
amazoncorretto:19
azul/zulu-openjdk:8
azul/zulu-openjdk:11
azul/zulu-openjdk:17
azul/zulu-openjdk:19
azul/prime:8
azul/prime:11
azul/prime:17
azul/prime:19
bellsoft/liberica-openjdk-debian:8
bellsoft/liberica-openjdk-debian:11
bellsoft/liberica-openjdk-debian:17
bellsoft/liberica-openjdk-debian:19
bitnami/java:11
bitnami/java:17
bitnami/java:17
devopology/dragonwell:8
devopology/dragonwell:11
devopology/dragonwell:17
devopology/openjdk:20
devopology/openlogic-openjdk:8
devopology/openlogic-openjdk:11
eclipse-temurin:8
eclipse-temurin:11
eclipse-temurin:17
eclipse-temurin:19
ghcr.io/graalvm/jdk:java8
ghcr.io/graalvm/jdk:java11
ghcr.io/graalvm/jdk:java17
ibmjava:8
ibmjava:11
ibm-semeru-runtimes:open-8-jdk-focal
ibm-semeru-runtimes:open-11-jdk-focal
ibm-semeru-runtimes:open-17-jdk-focal
konajdk/konajdk:8
konajdk/konajdk:11
mcr.microsoft.com/openjdk/jdk:8-mariner
mcr.microsoft.com/openjdk/jdk:11-mariner
mcr.microsoft.com/openjdk/jdk:17-mariner
mcr.microsoft.com/openjdk/jdk:11-mariner-cm1
mcr.microsoft.com/openjdk/jdk:17-mariner-cm1
mcr.microsoft.com/openjdk/jdk:11-ubuntu
mcr.microsoft.com/openjdk/jdk:17-ubuntu
openjdk:6
openjdk:8
openjdk:11
openjdk:17
openjdk:19
sapmachine:11
sapmachine:17
sapmachine:19
ticketfly/java:6
```

---

### Running the integration test suite

```
./mvnw clean package verify
```

**Notes**

- For any core `jmx_exporter` project changes, you have to build the core project and then build the `integration_test_suite` project


- The integration test suite uses the core `jmx_exporter` project jars as resources
  - This was preferred to allow the integration test suite to be autonomous  

---

Pull Docker images (not required, but you may see test timeouts)

```
./integration_test_suite/docker-pull-images.sh
```

**Notes**

- you may need to set up Docker hub login to pull images

---

Run integration tests using smoke test containers (requires building first)

```
./mvnw verify
```

**Notes**

- To test all containers, edit the `integration_tests/pom.xml` file and set the property `docker.image.names` to be empty

# Test time

Total test time is dependent on the number of test containers and hardware

| Hardware                                  | Smoke test containers | All test containers |
|-------------------------------------------|-----------------------|---------------------|
| AMD Ryzen 9 7900 + NVMe 4.0 Gen4 PCIe M.2 | ~3.5 minutes          | ~30 minutes         |
| Intel Core i5-3470 CPU + SATA SSD         | ~16 minutes           | 2+ hours            |

# General notes

- HTTPS should work for Java 6 containers, but is not tested
  - Java 6 containers are identified with a ":6" in the container name
  - Java 6 only supports insecure TLSv1.1 and TLSv1
