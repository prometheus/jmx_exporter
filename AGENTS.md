# Repository Guidelines

## Planning Guidelines

When creating a plan, read and use the repository-root `PLAN_TEMPLATE.md`. Copy it to `plans/<descriptive-name>.md`, creating `plans/` if needed, unless the user explicitly requests another location. Keep `PLAN_TEMPLATE.md` at the repository root; generated plans are local working documents in the Git-ignored `/plans/` directory and must not be force-added.

Replace placeholders with verified repository findings and concrete decisions, remove authoring notes, and scale detail to the change. Include the requested behavior, current implementation evidence, scope, design choices, ordered steps, relevant regression coverage, validation commands, acceptance criteria, and risks. Omit irrelevant sections or explain why they do not apply. Use the plans in `/home/dhoard/Development/github/dhoard/amanda/plans/` as examples of detail and evidence, adapting their approach to this repository and its template.

Record plan status, request scope, affected modules, and the evidence baseline. Reinspect current source and tests before execution; historical plans are not evidence of current behavior. Keep planned commands separate from actual validation results, and update the execution record as work proceeds. A plan-only request does not authorize implementation; follow existing user authorization and preserve unrelated worktree changes.

## Project Structure & Module Organization

This Maven multi-module project exports JMX metrics to Prometheus. `collector/` implements scraping and metric conversion; `jmx_prometheus_common/` provides shared configuration and HTTP support. `jmx_prometheus_javaagent/`, `jmx_prometheus_isolator_javaagent/`, and `jmx_prometheus_standalone/` provide deployment entry points. Modules ending in `-benchmarks/` contain JMH benchmarks.

Java sources and unit tests live in each module’s `src/main/java/` and `src/test/java/`; fixtures belong in `src/test/resources/`. `integration_test_suite/` contains the example application, integration tests, and metric expectations. `examples/` holds exporter YAML configurations; `website/` contains Docusaurus documentation and assets.

## Build, Test, and Development Commands

Use the Maven wrapper (Maven 3.9.6). Match CI with JDK 25; production code targets Java 8, while integration tests target Java 17. Docker is required for integration tests.

- `./mvnw clean verify`: build the reactor and run tests using the default quick Docker image set.
- `./mvnw -pl collector -am test`: run collector unit tests and build required modules.
- `./run-quick-test.sh`: pre-pull quick images, build, and test; output goes to `quick-test.log`.
- `./mvnw spotless:check` / `./mvnw spotless:apply`: check or apply Java formatting.
- From `website/`, run `npm ci`, then `npm run start` for local documentation or `npm run build` for validation.

Run the packaged standalone exporter with `java -jar jmx_prometheus_standalone/target/jmx_prometheus_standalone-<version>.jar 9404 exporter.yaml`.

## Coding Style & Naming Conventions

Use four-space Java indentation and the configured Palantir formatter. Keep existing Apache license headers. Use `io.prometheus.jmx` package namespaces, `UpperCamelCase` classes, `lowerCamelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Preserve Java 8 compatibility in production modules.

## Testing Guidelines

Unit tests use JUnit Jupiter and AssertJ; name classes `*Test` and methods descriptively, following existing `test...` names. Integration tests use Paramixel with Docker containers. Add regression coverage for behavior changes. JaCoCo generates coverage reports; no numeric coverage threshold is configured.

Review and commit generated expectations under `integration_test_suite/integration_tests/src/test/metrics/`. See the suite README for test filters and image overrides.

## Commit & Pull Request Guidelines

Recent commits use prefixes such as `fix:`, `chore:`, and `fix(deps):`; write concise, action-oriented subjects. Target PRs at `main`, describe behavior changes and validation, and link related issues. For trivial fixes, mention a maintainer listed in `MAINTAINERS.md`. Discuss substantial changes on the Prometheus developers mailing list first, as directed by `CONTRIBUTING.md`.
