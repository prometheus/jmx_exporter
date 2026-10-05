# JMX Exporter Implementation Plan — <outcome-focused title>

**Status:** <Draft / In progress / Complete / Blocked / Deferred / Superseded>
**Request scope:** <Plan only / Implementation requested; reference the user request>
**Affected modules:** <Collector, shared support, agent, isolator, standalone, integration tests, benchmarks, website>
**Related:** <Issue, prerequisite plans, superseded plans, or none>
**Evidence baseline:** <Date, branch/commit, and relevant worktree changes>

> Read `AGENTS.md` and copy this template to `./plans/<descriptive-name>.md`.
> Create `./plans/` if needed. Plans are local working documents: `/plans/` is Git ignored.
> Keep `PLAN_TEMPLATE.md` at the repository root; do not force-add generated plans.
> Use a different location only when explicitly requested by the user.
> Replace placeholders with verified findings and decisions, scale detail to the change,
> and remove these authoring notes. Omit irrelevant sections or explain why they do not apply.
> Use existing authorization; a plan-only request does not authorize implementation.
> Reinspect source and tests rather than treating historical plans as current behavior.

## 1. Goal and observable behavior

<Describe the problem and requested outcome. Distinguish reported symptoms from verified
failures. Define behavior precisely enough to validate it.>

- **Required behavior:** <Expected scraped metrics, configuration handling, HTTP response, or startup behavior.>
- **Failure behavior:** <Invalid configuration, unavailable MBean, authentication failure, or other relevant error.>
- **Compatibility:** <Existing metric names, labels, types, values, defaults, and deployment modes to preserve.>

Include a minimal exporter YAML, MBean example, HTTP request, or expected metric output when
helpful. Identify whether the change affects Java agent, isolator, standalone, HTTP, or
OpenTelemetry operation; use only the modes relevant to the request.

## 2. Repository guidance and behavior contract

Use the user request, `AGENTS.md`, `CONTRIBUTING.md`, current documentation, and verified code
and tests to establish the contract. Identify conflicts explicitly before dependent work.

| Reference | Applicable requirement | Consequence for this change |
| --- | --- | --- |
| User request / linked issue | <Requested outcome and constraints> | <Acceptance requirement> |
| `AGENTS.md` / `CONTRIBUTING.md` | <Style, compatibility, contribution guidance> | <Implementation or review requirement> |
| `website/docs/` / `examples/` | <Exact relevant configuration or deployment page> | <Documented behavior to preserve or revise> |
| `integration_test_suite/README.md` | <Fixtures, filters, metric expectations, image matrix> | <Integration validation strategy> |

**Open behavior decisions:** <None, or specific unresolved questions and evidence needed.>

## 3. Current implementation and evidence

Inspect the worktree before selecting an approach. Use actual paths and symbols.

| Evidence | Location / exact command | Verified finding |
| --- | --- | --- |
| Implementation | <Path and symbol> | <Current behavior and owning module> |
| Existing coverage / reproducer | <Test name or command> | <Actual result, or not run> |
| Build / packaging / documentation | <POM, script, workflow, or page> | <Affected consumers and constraints> |

- **Root cause / gap:** <Causal path; label unverified hypotheses.>
- **Infrastructure to retain:** <Shared helpers, caches, registry handling, lifecycle, shading, or test support.>
- **Baseline:** <Pre-existing failures, unrelated modified/untracked files, and unknowns.>

## 4. Scope and design

**In scope:** <Smallest coherent change, including necessary tests and documentation.>
**Out of scope:** <Unrelated cleanup and deferred improvements.>
**Affected files / symbols:** <Concrete paths and responsibilities.>

Use the following module map to identify ownership; retain only relevant rows in the plan.

| Module / directory | Responsibility | Planned change and invariant |
| --- | --- | --- |
| `collector/` | JMX scraping, rules, metric conversion | <Naming, labels, types, caching, scrape errors> |
| `jmx_prometheus_common/` | Shared configuration and exporter support | <Configuration defaults, HTTP lifecycle, authentication/TLS> |
| `jmx_prometheus_javaagent/` | In-process JVM agent | <Startup arguments, lifecycle, registry integration> |
| `jmx_prometheus_isolator_javaagent/` | Agent classloader isolation | <Loading and isolation boundaries> |
| `jmx_prometheus_standalone/` | Standalone process and remote JMX | <CLI, JMX connection, shutdown behavior> |
| `integration_test_suite/` | Example application, container scenarios, metric fixtures | <Deployment modes, image matrix, expected metrics> |
| `*-benchmarks/` | JMH performance measurements | <Relevant workload and comparison method> |
| `website/`, `examples/` | Documentation and sample configurations | <User-visible contract and examples> |

### Decisions and constraints

| Decision | Chosen approach | Rationale / alternatives considered |
| --- | --- | --- |
| <Implementation choice> | <Concrete design and owner> | <Evidence and tradeoff> |

- Preserve Java 8 compatibility in production modules; integration tests target Java 17.
- Follow Spotless with Palantir formatting and existing Apache license headers.
- Reuse shared support rather than duplicating behavior across deployment entry points.
- For dependency or packaging changes, inspect parent dependency management, relocations,
  service resources, and agent/standalone manifests as applicable.
- Explicitly account for metric cardinality, scrape overhead, concurrent access, and resource
  cleanup when the affected behavior requires it.

## 5. Ordered implementation steps

List dependency-ordered steps with concrete paths, behavior, and completion conditions.

1. **Reproducer and coverage — `<paths / tests>`**
   - <Reproduce the defect or define the requested behavior at its owning boundary.>
   - Done when: <The expected result and existing gap are demonstrated.>
2. **Implementation — `<paths / symbols>`**
   - <Make the coherent change across affected modules.>
   - Done when: <Focused checks pass and compatibility requirements are met.>
3. **Integration and documentation — `<fixtures / pages / examples>`**
   - <Align applicable deployment modes, resource configurations, and documentation.>
   - Done when: <Tests and documented behavior agree.>
4. **Review and validation**
   - <Review the complete diff, resolve findings, and run the selected checks below.>
   - Done when: <Acceptance items and actual validation results are recorded.>

Reinspect `git status --short` before execution and preserve unrelated user work.
Stage, commit, push, or publish only within the user's authorized scope.

## 6. Coverage and fixtures

Use JUnit Jupiter and AssertJ for unit tests, and Paramixel with Docker containers for
integration scenarios. Follow existing `*Test` classes and descriptive `test...` methods.
Choose coverage that verifies behavior rather than mirroring implementation details.

| Scenario | Expected result | Test path / name and fixture |
| --- | --- | --- |
| Normal operation | <Metrics or response> | <Unit or integration test> |
| Invalid input / failure | <Validation error or documented failure behavior> | <Negative regression> |
| Boundary / concurrency / cleanup | <Stable behavior and resource lifecycle> | <Relevant test> |
| Deployment modes | <Applicable JavaAgent / Standalone / isolator behavior> | <Mode-specific resources> |
| Exposition formats | <Applicable Prometheus text, OpenMetrics, Protobuf behavior> | <Content-type assertions> |

Integration configurations and scripts belong under
`integration_test_suite/integration_tests/src/test/resources/<test-class-package>/mode/`.
Metric expectations belong under `integration_test_suite/integration_tests/src/test/metrics/`,
grouped by test class, mode, and sanitized image name.

Missing metric expectation files are generated on first run. Review every generated or updated
file before committing it; retain wildcards for runtime-dependent values. Use
`-Dmetric.assertions.update=true` only for intentional expectation changes. Do not regenerate
fixtures merely to make unexpected behavior pass. JaCoCo reports coverage; no numeric threshold
is configured. Explain material coverage exclusions.

## 7. Verification and acceptance

### Planned commands — not results

Run from the repository root unless noted. Use the Maven wrapper (Maven 3.9.6) and match CI
with JDK 25. Docker is needed for integration checks; documentation CI uses Node.js 24.
Select checks appropriate to the affected behavior and replace placeholders before execution.

| Purpose | Command | When applicable |
| --- | --- | --- |
| Formatting | `./mvnw spotless:check` | Java changes; use `spotless:apply` to format |
| Focused unit tests | `./mvnw -pl <module> -am -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false test` | Select an actual unit-test module/class |
| Focused integration | `./run-quick-test.sh '-Dparamixel.match.class.regex=.*<TestClass>'` | Docker regression; regex uses full-string matching |
| CI quick suite | `./run-quick-test.sh` | Production or build changes; pre-pulls quick images, builds, tests |
| Smoke / full matrix | `./run-smoke-test.sh` / `./run-regression-test.sh` | Broader runtime compatibility; full matrix can take hours |
| Documentation | `./scripts/build-documentation.sh` | Website changes; follows documentation CI |
| Diff hygiene | `git diff --check` | All changes |

`./mvnw clean verify` builds and tests the reactor with the default quick image selection.
`JAVA_DOCKER_IMAGES` and `PROMETHEUS_DOCKER_IMAGES` accept `QUICK`, `SMOKE`, `ALL`, or explicit
images. Record chosen images and filters. Add JMH comparisons when needed to substantiate a
performance change; include workload, JDK, and measurement settings.

For Markdown-only planning changes, review paths, commands, and diff hygiene; application
suites are unnecessary. For production/build changes, plan formatting and the CI quick suite
plus relevant focused checks. Explain unavailable prerequisites or omitted checks. Do not
report compilation or a filtered test run as validation of the full suite.

### Acceptance checklist

- [ ] Requested behavior and compatibility requirements are satisfied.
- [ ] Relevant regression and failure cases pass in the applicable modes/formats.
- [ ] Configuration examples, documentation, and reviewed metric expectations agree.
- [ ] Selected validation commands pass; any unavailable checks are explicitly recorded.
- [ ] Diff reviewed; `git diff --check` passes; unrelated user work is preserved.
- [ ] Actual results are recorded separately from planned commands.

## 8. Risks, open decisions, and follow-ups

| Risk / question | Impact | Evidence / mitigation / decision | Blocking? |
| --- | --- | --- | --- |
| <Compatibility, cardinality, concurrency, TLS, shading, or image variability> | <Failure mode> | <Evidence or resolved choice> | <Yes / no> |

Resolve behavior-defining questions before dependent implementation. Separate optional follow-ups
from acceptance requirements and describe rollback considerations when relevant.

## 9. Execution record and handoff

- **Implemented:** <Behavior and files changed, or “plan only; no implementation performed.”>
- **Deviations:** <Changes from the design and rationale, or none.>
- **Fixtures / documentation updated:** <Paths, or none with reason.>
- **Diff review:** <Findings resolved and unrelated changes preserved.>

| Exact command actually run | Actual result | Evidence / failure detail |
| --- | --- | --- |
| <Command, working directory, filters/image overrides> | <Passed / failed / blocked> | <Log/report path or observed result> |

**Not run:** <Checks and concrete reasons; never label unrun checks as passed.>
**Remaining work:** <Unresolved acceptance items, blockers, limitations, and optional follow-ups.>
**Completion:** <Plan-only deliverable, or implementation complete after selected required checks
and acceptance criteria pass.>
