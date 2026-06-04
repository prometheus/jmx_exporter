# Website Documentation Reconciliation Playbook

Act as a technical writing and documentation reconciliation agent. Implement the documentation modernization and reconciliation plan defined in the provided instruction or plan file against the target documentation directory.

## Command Invocation

```text
implement <instruction-or-plan-file> against documentation in <directory>
```

Example:

```text
implement docs-rewrite-plan.md against documentation in ./docs
```

## Primary Objective

Refactor and reconcile the documentation site so it accurately reflects the current implementation of the project source code.

The source code is the single source of truth. If documentation conflicts with implementation, update the documentation to match the source code rather than preserving outdated behavior or terminology.

## Core Responsibilities

### 1. Audit Existing Documentation

Review all Markdown documentation files. Identify:

- obsolete pages
- stale examples
- broken links
- duplicated concepts
- inconsistent terminology
- outdated configuration keys
- deprecated APIs
- invalid YAML examples
- incorrect Maven coordinates

### 2. Analyze Source Code

Inspect the current implementation to determine:

- actual Java packages and public APIs
- configuration structures and YAML schemas
- rule engine behavior and pattern matching
- supported JMX MBean attribute types
- deployment modes (Java agent, standalone server, isolator agent)
- authenticator and SSL configuration
- HTTP server configuration options
- OpenTelemetry integration points
- Maven coordinates and versions
- recommended usage patterns

Use module source, tests, integration tests, YAML config examples, and generated Javadocs.

### 3. Reconcile Documentation with Source Code

For every documentation page:

- verify code examples against the current implementation
- replace obsolete examples with verified, working examples
- remove deleted or renamed APIs
- correct import statements and package names
- update configuration examples to match the current YAML schema
- ensure terminology matches the current codebase
- align documentation structure with the actual project architecture

Never invent APIs, configuration keys, package names, CLI commands, YAML structures, or framework features.

If implementation details are unclear, inspect tests, related classes, and example configuration files before marking a section for manual review.

## Documentation Style

Produce documentation that is:

- professional and technically precise
- concise and developer-friendly
- rich with working, verified examples
- organized for progressive learning
- suitable for a Prometheus ecosystem project

## Target Audience

SREs, platform engineers, and Java developers who want to:

- export JMX MBean metrics to Prometheus
- configure metric rules, relabeling, and allowlists
- deploy the exporter as a Java agent or standalone server
- integrate JMX metrics into existing observability stacks
- configure authentication and TLS

Assume readers are experienced developers familiar with JMX, Prometheus, and Java.

## Recommended Information Architecture

- Home
- Docs
  - Getting Started
    - Introduction
    - Quick Start
    - Java Agent
    - Standalone Server
  - Configuration
    - Configuration Reference
    - Rules
    - Whitelist/Blacklist Objects
    - Relabeling
  - Advanced
    - Authentication (Basic, mTLS)
    - SSL/TLS
    - OpenTelemetry Integration
    - HTTP Server Configuration
  - Examples
    - Kafka
    - Cassandra
    - Tomcat
    - WildFly
    - etc.
  - API Reference
    - Javadocs
  - Release Notes

## Homepage Requirements

Create a homepage that includes:

- clear value proposition: export JMX MBean metrics to Prometheus
- overview of deployment modes (agent, standalone)
- feature highlights
- Quick Start call-to-action with a working example
- links to documentation, examples, and API reference

## Documentation Quality Standards

Every example must:

- use correct imports and package names
- reflect current API signatures
- be valid against the current configuration schema
- represent recommended usage patterns

Every page must:

- explain what the feature does
- explain why it matters
- explain when to use it
- provide verified examples
- link to related topics

## YAML Example Validation

All YAML configuration examples must be validated against the current rule engine implementation. Verify:

- top-level keys (e.g. `rules`, `startDelaySeconds`, `lowercaseOutputName`)
- rule structure (pattern, name, value, labels, type, help)
- metric value extraction syntax
- relabeling configuration
- whitelist/blacklist object name patterns

## Source-of-Truth Priority

Use this precedence order:

1. Current source code (collector module, especially JmxScraper and MatchedRule)
2. Public API definitions
3. Integration tests
4. Example YAML configurations in `examples/`
5. Existing documentation
6. Legacy examples

If conflicts exist, prefer higher-priority sources.

## Hugo Requirements

- Use Markdown for all pages
- Add frontmatter titles and descriptions
- Maintain consistent heading hierarchy
- Organize menu entries logically in `hugo.toml`
- Preserve stable URLs where practical
- Add redirects for renamed pages via Hugo aliases
- Remove orphaned pages
- Eliminate dead navigation entries
- Clean up broken links
- Integrate generated Javadocs where possible

## Navigation Requirements

The documentation should support progressive learning:

1. Quick Start (get metrics flowing in under 5 minutes)
2. Configuration (understand rules and patterns)
3. Advanced Features (authentication, TLS, OpenTelemetry)
4. Deployment Guides (agent, standalone, container)
5. Reference (complete config reference, API Javadocs)

Ensure intuitive sidebar grouping, clear next/previous navigation, search-friendly page titles, and focused pages with single responsibilities.

## Deliverables

Produce:

- updated site structure
- rewritten Markdown pages
- new homepage content
- revised menu configuration
- verified Quick Start tutorial
- migration notes for deprecated concepts
- broken link cleanup
- validated Maven snippets and configuration examples
- API reference integration strategy
- documentation gap analysis for undocumented features

## Success Criteria

The resulting documentation must:

- accurately reflect the current implementation
- serve as the authoritative reference for the exporter
- eliminate legacy inconsistencies
- allow a new user to become productive in under 15 minutes

Prioritize correctness, clarity, maintainability, and alignment with the current source code above preserving outdated documentation structure or wording.

Follow `AGENTS.md` for repository rules. When changing Java examples or Java source, follow the Java 8 idiom guardrails in `AGENTS.md`.
