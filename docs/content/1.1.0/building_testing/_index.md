---
title: "Building / Testing"
weight: 4
---

Java 11+ is required to build and test the code.

Docker is required to run the integration test suite.

## Building (unit tests only)

### Commands

```shell
git clone https://github.com/prometheus/jmx_exporter
cd jmx_exporter
./mvnw clean package
```

### Example Output

```shell
...
[INFO] Reactor Summary for Prometheus JMX Exporter <VERSION>:
[INFO]
[INFO] Prometheus JMX Exporter ............................ SUCCESS [  1.049 s]
[INFO] Prometheus JMX Exporter - Collector ................ SUCCESS [ 12.947 s]
[INFO] Prometheus JMX Exporter - Common ................... SUCCESS [  3.792 s]
[INFO] Prometheus JMX Exporter - Java Agent ............... SUCCESS [ 44.846 s]
[INFO] Prometheus JMX Exporter - Standalone Server ........ SUCCESS [  7.049 s]
[INFO] Prometheus JMX Exporter - Integration Test Suite ... SUCCESS [  0.278 s]
[INFO] Prometheus JMX Exporter - JMX Example Application .. SUCCESS [  1.925 s]
[INFO] Prometheus JMX Exporter - Integration Tests ........ SUCCESS [  4.505 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:17 min
[INFO] Finished at: 2024-12-02T13:39:20-05:00
[INFO] ------------------------------------------------------------------------
```

## Integration Testing (smoke test containers)

Integration tests use [Verifyica](https://github.com/verifyica-team/verifyica) for integration testing.

Integration tests require Docker configuration changes due to parallel testing/the number of Docker networks used during testing.

**Integration testing time using smoke test containers varies based on your machine specifications.**

- **~7 minutes on an AMD Ryzen 9 7900 with an NVMe drive**
- **~20 minutes using a standard GitHub action runner**

**Notes**

- You may need to set up your Docker hub login to pull images

### Docker Configuration

Create a Docker `daemon.json` file.

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

Restart Docker.

### Commands

```shell
git clone https://github.com/prometheus/jmx_exporter
cd jmx_exporter
./integration_test_suite/pull-smoke-test-docker-images.sh
./mvnw clean verify
```

**Notes**

- The `smoke-test.sh` shell script can be used to build and run the integration test suite using smoke test containers.
  - `./smoke-test.sh`
- Output is captured and logged to `smoke-test.log`.

### Example Output

```shell
[INFO] ------------------------------------------------------------------------
[INFO] Verifyica 0.7.2 Summary (2024-12-02T13:53:18-05:00)
[INFO] ------------------------------------------------------------------------
[INFO] Test classes   :   40 Passed :   40 Failed : 0 Skipped : 0
[INFO] Test arguments :  506 Passed :  506 Failed : 0 Skipped : 0
[INFO] Test methods   : 2302 Passed : 2302 Failed : 0 Skipped : 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] TESTS PASSED
[INFO] ------------------------------------------------------------------------
[INFO] Compact Summary | 40 40 0 0 | 506 506 0 0 | 2302 2302 0 0 | 387351.971080 ms | P
[INFO] ------------------------------------------------------------------------
[INFO] Total time  : 6 m, 27 s, 351 ms (387351.97108 ms)
[INFO] Finished at : 2024-12-02T13:59:47-05:00
[INFO] ------------------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for Prometheus JMX Exporter <VERSION>>:
[INFO]
[INFO] Prometheus JMX Exporter ............................ SUCCESS [  0.755 s]
[INFO] Prometheus JMX Exporter - Collector ................ SUCCESS [  9.543 s]
[INFO] Prometheus JMX Exporter - Common ................... SUCCESS [  3.092 s]
[INFO] Prometheus JMX Exporter - Java Agent ............... SUCCESS [ 35.511 s]
[INFO] Prometheus JMX Exporter - Standalone Server ........ SUCCESS [  5.672 s]
[INFO] Prometheus JMX Exporter - Integration Test Suite ... SUCCESS [  0.259 s]
[INFO] Prometheus JMX Exporter - JMX Example Application .. SUCCESS [  1.488 s]
[INFO] Prometheus JMX Exporter - Integration Tests ........ SUCCESS [06:42 min]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  07:39 min
[INFO] Finished at: 2024-12-02T13:59:47-05:00
[INFO] ------------------------------------------------------------------------
```

## Integration Tests (all test containers)

Integration tests use [Verifyica](https://github.com/verifyica-team/verifyica) for integration testing.

Integration tests require Docker configuration changes due to parallel testing/the number of Docker networks used during testing.

**Integration testing using all test containers requires SIGNIFICANT time and disk space.**

- **~100 Docker containers (Java and Prometheus containers)**
- **~2 hours on an AMD Ryzen 9 7900 + NVMe**
- **3+ hours on a Dual Intel Xeon CPU E5-2680 v4**

**Notes**

- You may need to set up your Docker hub login to pull images

### Docker Configuration

Create or edit...

```shell
/etc/docker/daemon.json
```

**Content**

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

Restart Docker.

### Commands

```shell
git clone https://github.com/prometheus/jmx_exporter
cd jmx_exporter
./integration_test_suite/pull-smoke-test-docker-images.sh
./mvnw clean verify
```

### Scripts

Test scripts are available to build and run various integration test suite scenarios

- [run-quick-test.sh](https://github.com/prometheus/jmx_exporter/blob/main/run-quick-test.sh)
- [run-smoke-test.sh](https://github.com/prometheus/jmx_exporter/blob/main/run-smoke-test.sh)
- [run-regression-test.sh](https://github.com/prometheus/jmx_exporter/blob/main/run-regression-test.sh)
- [run-targeted-test.sh](https://github.com/prometheus/jmx_exporter/blob/main/run-targeted-test.sh)
- [run-stress-test.sh](https://github.com/prometheus/jmx_exporter/blob/main/run-stress-test.sh)

### Output

Output is similar to integration testing using smoke test containers.
