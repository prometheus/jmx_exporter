# Agent Instructions

Provider-neutral instructions for coding agents working in this repository. These rules apply regardless of LLM provider, model family, editor, or automation runtime.

## Critical Rules

- Before any Maven validation/build command (`test`, `check`, `package`, `install`, `verify`, `javadoc`, etc.), run `./mvnw spotless:apply`. The only exception is an explicitly requested read-only formatting check with `./mvnw spotless:check`.
- Integration tests in `integration_test_suite/` require Docker. Use `-pl '!integration_test_suite'` to skip them when Docker is unavailable.
- Preserve Java 8 compatibility for all production modules. Only `integration_test_suite/integration_tests` uses Java 17.
- Preserve existing public API/exception semantics unless the user explicitly asks for a breaking change.
- Do not weaken Spotless, strict Javadoc, test, or build configuration to make validation pass.
- Prefer the smallest safe change and report the commands run with pass/fail results.

## Standard Agent Workflow

1. Inspect the relevant source, tests, build files, and repository guidance before changing files.
2. Make the smallest focused change that satisfies the request.
3. Run `./mvnw spotless:apply` before Maven validation.
4. Run the narrowest relevant validation command from the table below.
5. Prefer `./mvnw clean verify` when touching shared/core behavior or build configuration.
6. Summarize changed files, validation commands, results, and any remaining risks.

## Validation Commands

Run `./mvnw spotless:apply` first for validation/build commands unless the task is a read-only formatting check.

| Task | Command |
| --- | --- |
| Format code | `./mvnw spotless:apply` |
| Full Maven validation (requires Docker) | `./mvnw clean verify` |
| Unit tests only (no Docker) | `./mvnw test -pl '!integration_test_suite'` |
| Single module unit tests | `./mvnw test -pl collector` |
| Integration tests only (requires Docker) | `./mvnw test -pl integration_test_suite/integration_tests` |
| Build without running any tests | `./mvnw clean install -DskipTests -pl '!integration_test_suite'` |
| Check formatting only | `./mvnw spotless:check` |
| Check Javadoc only | `./mvnw javadoc:javadoc` |
| Full build (requires Docker) | `./mvnw clean install` |

`-DskipTests` skips Surefire JUnit tests in standard test sources. Integration tests in `integration_test_suite/integration_tests` use the Paramixel Maven plugin bound to the `test` phase, with Surefire explicitly skipped in that module.

## Module Structure

- `collector/` — Core JMX collector library, deploys to Maven Central
- `jmx_prometheus_common/` — Shared utilities: HTTP server setup, YAML config parsing, authenticators
- `jmx_prometheus_javaagent/` — Java agent for in-process JMX metric export over HTTP
- `jmx_prometheus_standalone/` — Standalone HTTP server for remote JMX scraping
- `jmx_prometheus_isolator_javaagent/` — Alternate Java agent variant
- `integration_test_suite/` — Aggregator POM for integration tests (not deployed)
  - `jmx_example_application/` — Example application JAR used as test target
  - `integration_tests/` — Testcontainers-based integration tests (Java 17, requires Docker)

## Testing Rules

### Unit Tests

Unit tests live under `src/test/java/` in each production module. They use:
- **JUnit 5** (Jupiter) — `org.junit.jupiter.api.Test`, `@BeforeEach`, `@TempDir`, etc.
- **AssertJ** — `org.assertj.core.api.Assertions.assertThat`
- **Mockito** — `org.mockito` (used in `collector` module)

### Integration Tests

Integration tests live in `integration_test_suite/integration_tests/` and use:
- **Paramixel** test framework (`org.paramixel:core`) — test classes have a `@Paramixel.Factory` static method and a `main()` entry point
- **Testcontainers** — Docker-based test environments
- **Java 17** — compiled separately from production code

Each test package contains a `__ParamixelRunner__` class with a `main()` method:
```java
public class __ParamixelRunner__ {
    public static void main(String[] args) {
        Runner.defaultRunner().runAndExit(Selector.packageTreeOf(__ParamixelRunner__.class));
    }
}
```

### Quick Feedback Loop

For fast development cycles, use the smoke test script:

```bash
./run-quick-test.sh
```

This pulls a minimal set of Docker images and runs a subset of integration tests.

For repeated runs to detect flaky tests:

```bash
./run-stress-test.sh <number-of-iterations>
```

## Code Style

- Spotless with Palantir Java Format runs on the `compile` phase (`spotless:check`). Run `./mvnw spotless:apply` manually to fix formatting.
- License header from `license-header.txt` (Apache 2.0) is required on all Java files.
- Production code follows Java 8 idioms. See `.ai/prompts/java-8-idioms.md` for the full coding standards and modernization audit workflow.
- Integration test code (`integration_test_suite/integration_tests`) may use Java 17 features. See `.ai/prompts/java-17-review.md` for the review checklist.

## Javadoc

- Maven Javadoc plugin runs during the `package` phase.
- Javadoc JARs are generated only in the `release` profile (for `collector`, `jmx_prometheus_javaagent`, and `jmx_prometheus_standalone`).
- Missing `@param`, `@return`, or `@throws` tags will fail the build when Javadoc is active.

## Planning

Plans go in `.ai/plans/`. See `.ai/prompts/planning-workflow.md` for naming conventions.

## Commit Requirements

- DCO: All commits must be signed off with `git commit -s`.
- Conventional commit prefixes: `feature:`, `fix:`, `refactor:`, `chore:`, `performance:`, `polish:`.
- Dependency updates use scoped prefix: `chore(deps):` or `fix(deps):`.

## Release

Manual release with CI validation. See `RELEASING.md` for the release source of truth. Requires `~/.m2/settings.xml` (Maven Central credentials) and GPG signing key.

## Docker

Integration tests require Docker. Pull images before running the full test suite:

```bash
# Pull all Docker images used in integration tests
cd integration_test_suite && ./pull-docker-images.sh

# Pull only smoke test images (faster)
cd integration_test_suite && ./pull-smoke-test-docker-images.sh
```

## Stress Testing

Run tests multiple times to check for flaky tests:

```bash
./run-stress-test.sh <number-of-iterations>
```

## Maven Version

Requires Maven 3.9.6+ (enforced by enforcer plugin).

## Java Compatibility

- Source/target/release: 8 (production modules), 17 (`integration_test_suite/integration_tests`)
- CI tests against Java 17, 21, 25
