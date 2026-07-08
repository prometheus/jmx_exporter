# Integration Test Suite

Docker-based integration tests powered by [Paramixel](https://www.paramixel.org) that verify the
JMX exporter produces correct metrics in Prometheus text, OpenMetrics text, and Protobuf formats
across multiple Java Docker images, exporter modes (JavaAgent / Standalone), and configurations.

## Architecture

### Directory layout

```
integration_test_suite/
├── jmx_example_application/              # Example JMX app deployed in containers
├── integration_tests/
│   ├── src/main/java/.../support/        # Infrastructure (environments, parsers, assertions)
│   ├── src/test/java/.../
│   │   ├── core/                         # Core JMX scraping feature tests
│   │   ├── http/authentication/          # HTTP auth tests
│   │   ├── http/ssl/                     # HTTP SSL/TLS tests
│   │   ├── http/threads/                 # HTTP thread pool tests
│   │   ├── http/metrics/path/            # Custom metrics path tests
│   │   ├── opentelemetry/                # OpenTelemetry integration tests
│   │   └── rmi/ssl/                      # RMI SSL connection tests
│   ├── src/test/resources/               # Classpath resources
│   │   ├── <TestClass>/
│   │   │   └── mode/
│   │   │       ├── JavaAgent/            # application.sh, exporter.yaml
│   │   │       └── Standalone/           # application.sh, exporter.sh, exporter.yaml
│   │   ├── java-docker-images.txt        # Docker images to test against
│   │   └── smoke-test-java-docker-images.txt
│   └── src/test/metrics/                 # Metric assertion files (auto-generated, file I/O)
│       └── <test-class-package-path>/
│           ├── JavaAgent/
│           │   └── <sanitized-docker-image>.txt
│           └── Standalone/
│               └── <sanitized-docker-image>.txt
└── README.md
```

Two resource directories exist for different purposes:

- **`src/test/resources/`** — classpath resources loaded via `Class.getResourceAsStream()` (exporter
  YAML, shell scripts, keystores, Docker image lists).
- **`src/test/metrics/`** — metric assertion `.txt` files accessed via `Files.readString` /
  `Files.writeString`. Grouped per test class, exporter mode, and sanitized Docker image name.

### Test environment (`JmxExporterTestEnvironment`)

The test environment wraps Testcontainers to spin up Docker containers per test:

- **JavaAgent mode** — a single container runs the example application with the JMX exporter as an
  in-process JVM agent. Exposes port 8888 for metrics.
- **Standalone mode** — two containers: one for the example application (port 9999) and one for the
  standalone exporter process (port 8888).

Docker images are sourced from:

- `java-docker-images.txt` (full set)
- `smoke-test-java-docker-images.txt` (quick subset)

The `JAVA_DOCKER_IMAGES` environment variable overrides (e.g., `azul/zulu-openjdk:17` or `ALL`).

Each test runs once per combination of Docker image x exporter mode.

### Paramixel test lifecycle

Each test class has a `@Paramixel.Factory` static method and a `main()` entry point. The lifecycle
is:

1. `setUp()` — create Docker network, initialize environment (starts containers).
2. `testHealthy()` — verify `/healthy` returns 200.
3. `testDefaultTextMetrics()` / `testOpenMetricsTextMetrics()` /
   `testPrometheusTextMetrics()` / `testPrometheusProtobufMetrics()` — HTTP GET with appropriate
   `Accept` header, parse the response, assert against the metric assertion file.
4. `tearDown()` — stop containers, close network.

Tests run in parallel per environment. Each environment is identified by its mode and Docker image
(e.g., `JavaAgent(amazoncorretto:17)`).

## Metric assertion files

### What they are

Line-oriented `.txt` files that describe expected metrics using `match`, `present`, and `absent`
directives. They replace the older approach of inline `MetricAssertion` fluent calls per test.

Key properties:

- **Self-generating** — on first run, if no assertion file exists, it is created from the actual
  scraped metrics. Subsequent runs verify against it.
- **One file per Docker image** — separate assertion files per JDK vendor/version because metric
  names and values can differ across JVM implementations.
- **Reusable across content types** — the same assertion file validates Prometheus text, OpenMetrics
  text, and Protobuf responses.

### File format

```
# metric-assertions-version 1
# io.prometheus.jmx.test.core.BasicTest JavaAgent amazoncorretto:17

match GAUGE jmx_exporter_build_info{name="jmx_javaagent"} 1.0
match COUNTER jmx_scrape_error 0.0
match UNTYPED java_lang_runtime_uptime *
present jmx_
absent com_sun_
```

| Directive | Meaning |
|---|---|
| `match TYPE name 1.23` | Metric must exist with exact type, name, and value. No labels. |
| `match TYPE name{key="val"} 1.23` | Metric must exist with exact type, name, labels, and value. |
| `match TYPE name{key="val"} *` | Metric must exist; any value accepted (wildcard). |
| `match TYPE name *` | Metric must exist without labels; any value accepted. |
| `present PREFIX` | At least one metric with this prefix must exist. |
| `absent PREFIX` | No metric with this prefix may exist. |
| `# ...` | Comment (version header, context line, or blank). |

Label values use Prometheus-style escaping: `\"`, `\\`, `\n`, `\t`. Non-ASCII characters are
written as UTF-8.

### Runtime-specific metric handling

Two configurable prefix sets in `MetricsAssertions` control how runtime-varying metrics are
handled:

**Excluded metrics** (`EXCLUDE_METRIC_NAME_PREFIXES`) — omitted from assertion files entirely
because they are vendor-specific, GC-specific, or JDK-version-specific. Examples: `com_ibm_*`,
`org_graalvm_*`, `java_lang_G1_*`, `java_lang_ZGC_*`, `java_lang_MemoryPool_*`, `java_nio_*`.

**Runtime-specific values** (`RUNTIME_SPECIFIC_VALUE_PREFIXES`) — included in assertion files but
with `*` (wildcard) instead of a numeric value because the value depends on the runtime
environment. Examples: `java_lang_*`, `jvm_*`, `process_*`, `jmx_build_info`,
`jmx_scrape_duration_*`.

### How generation and verification work

When `MetricsAssertions.assertMetrics(testClass, mode, javaDockerImage, metrics)` is called:

1. **Compute path**: `<test-class-package>/<mode>/<sanitized-docker-image>.txt` under
   `src/test/metrics/`.
2. **File missing** -> write it from the scraped metrics (sorted, with excluded metrics filtered
   out and runtime values wildcarded), then verify.
3. **File present** -> read and verify. Verification checks that every `match` directive
   corresponds to exactly one metric with matching type, name, labels, and value (or any value
   for `*`). `present` / `absent` directives check for existence / absence of any metrics with
   the given prefix.

**Force-regeneration**: set system property `metric.assertions.update=true` or environment
variable `METRIC_ASSERTIONS_UPDATE=true`.

**Write directory resolution**:

1. `metricAssertions.write.dir` system property (custom path).
2. `integration_test_suite/integration_tests/src/test/metrics` (default, when running from repo
   root).
3. `src/test/metrics` (fallback when running from the `integration_tests/` module directory).

### `MetricsAssertions` API reference

| Static method | Purpose |
|---|---|
| `assertMetrics(Class, String, String, Collection<Metric>)` | Main assertion entry point |
| `assertMetrics(Class, String, String, Map<String,Collection<Metric>>)` | Variant with name-grouped metrics |
| `loadMetricNames(Class, String, String)` | Returns set of metric names from the assertion file |
| `assertMetricsContentType(HttpResponse, MetricsContentType)` | Validates HTTP response has expected content type and 200 status |

## Running tests

### Prerequisites

- **Docker** — required for all integration tests.
- **Maven 3.9.6+** — enforced by the Maven Enforcer plugin.

### Maven commands

```bash
# Full suite (smoke test Docker images — minutes)
./mvnw clean verify

# Full suite (all Docker images — hours)
export JAVA_DOCKER_IMAGES=ALL
./mvnw clean verify

# Single Docker image
JAVA_DOCKER_IMAGES="azul/zulu-openjdk:17" ./mvnw clean verify

# Filter by test class name (Paramixel regex — full-string match)
./mvnw clean install -Dparamixel.match.class.regex='.*BasicTest'

# Filter by package
./mvnw test -pl integration_test_suite/integration_tests \
  -Dparamixel.match.package.regex='io\.prometheus\.jmx\.test\.core(\..*)?'

# Quick development feedback loop
./run-quick-test.sh

# Stress test for flaky test detection
./run-stress-test.sh <number-of-iterations>
```

Paramixel regex filters use `Matcher.matches()` semantics (full-string match). Use `.*`
prefix/suffix for contains-style matching.

### Pulling Docker images (optional)

Pre-pulling avoids request timeouts during test runs:

```bash
# Smoke test images (faster)
./integration_test_suite/pull-smoke-test-docker-images.sh

# All images
./integration_test_suite/pull-docker-images.sh
```

### Testing via the IDE

#### Running integration tests

Each test package contains a `__ParamixelRunner__` class with a `main()` method. Running a
`__ParamixelRunner__` from an IDE executes all tests in that package.

#### Running local (non-Docker) tests

`LocalTest.java` is a non-Docker test that starts a JMX Exporter locally and verifies
functionality with multiple simultaneous HTTP clients. It can be run via `LocalTest.main()` from
an IDE.

### Docker network configuration

When running the integration test suite, Docker may need to be configured to support a large
number of network addresses.

On Linux, edit `/etc/docker/daemon.json`:

```json
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

## Writing a new integration test

1. **Create the test class** in the appropriate package under
   `integration_tests/src/test/java/io/prometheus/jmx/test/`. Follow the Paramixel pattern:
   `@Paramixel.Factory` static method returning an `Action`, `main()` deleg to
   `Runner.defaultRunner().runAndExit(factory())`, and instance methods for `setUp()`, test
   methods, and `tearDown()`. Copy the structure from an existing test such as
   `core/BasicTest.java` or `core/LowerCaseOutputLabelNamesTest.java`.

2. **Create test resources** under `src/test/resources/<test-class-package>/mode/`:
   - `mode/JavaAgent/exporter.yaml` — exporter configuration for JavaAgent mode.
   - `mode/Standalone/exporter.yaml` — exporter configuration for Standalone mode.
   - Optionally, `mode/<Mode>/application.sh` and `mode/<Mode>/exporter.sh` if custom container
     startup scripts are needed.

3. **Write test methods**. Each test should:
   - Use `HttpClient.sendRequest(url)` to fetch metrics.
   - Call `MetricsAssertions.assertMetricsContentType(response, contentType)`.
   - Parse with `MetricsParser.parseMap(response)`.
   - Call `MetricsAssertions.assertMetrics(TestClass.class, javaDockerImage, mode, metrics)`.

4. **Generate assertion files** by running the test once. Files are auto-created under
   `src/test/metrics/<test-class-package>/<mode>/`.

5. **Review and commit** the generated `.txt` files. Verify that:
   - All expected application metrics appear in the file.
   - Runtime-varying JVM metrics have `*` as their value (not a fixed number).
   - GC-specific and vendor-specific metrics are absent (excluded).
   - No unintended metrics are present.

## Updating assertion files after exporter changes

When exporter behavior changes and assertion files are stale:

```bash
# Option A: Delete and regenerate
find integration_test_suite/integration_tests/src/test/metrics -name '*.txt' -delete
./mvnw clean verify

# Option B: Force-update in-place
./mvnw clean verify -Dmetric.assertions.update=true
```

After regeneration:

1. `git diff` the `.txt` files to review every change.
2. Confirm added metrics are expected; confirm removed metrics are intentional.
3. Commit the updated files.

## Notes

- You may need to set up Docker Hub login to pull images.
- Testing all Docker images requires a significant amount of time (hours).
- For development, use `./run-quick-test.sh` for a faster test cycle.
