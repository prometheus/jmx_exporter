# Paramixel Javadoc Style Guide

## Purpose

This guide defines the standard for writing professional Javadoc in Paramixel, with primary emphasis on code in `core/src/main`.

The goals are to:
- explain API intent clearly
- document behavior and contracts precisely
- make generated Javadoc useful to end users and maintainers
- keep documentation consistent across packages and modules

---

## General Principles

- Write Javadoc for the reader, not for the compiler.
- Describe what the API does, when it should be used, and any important constraints.
- Prefer precise behavioral language over repeating method or class names.
- Document externally visible contracts, side effects, and failure modes.
- Keep summaries concise, but include detail where behavior is non-obvious.

---

## Required Coverage

### Public and protected types
Every public or protected type should have class-level or interface-level Javadoc.

Document:
- the role of the type
- where it fits in Paramixel
- important lifecycle, hierarchy, or execution semantics
- whether it is public API, advanced SPI, or primarily internal support

### Public and protected members
Every public or protected method should have meaningful Javadoc unless the inherited documentation is already sufficient and intentionally relied upon.

Document as applicable:
- `@param`
- `@return`
- `@throws`
- type parameters
- behavioral notes
- side effects

### Constants
Public constants should explain:
- what they configure or represent
- valid values where relevant
- default behavior when unset
- related APIs that consume them

---

## Summary Sentence Rules

The first sentence should stand on its own in generated summaries.

Prefer:
- “Executes child actions concurrently.”
- “Returns the effective configuration for the current run.”
- “Signals that an action should be marked as skipped.”

Avoid weak summaries like:
- “Gets the name.”
- “This method executes the action.”
- “Utility class for configuration.”

---

## Method Documentation Rules

### Parameters
Use `@param` for every meaningful parameter.
Explain the role of the parameter, not just its type.

Good:
- `@param composition the composition strategy for multiple discovered actions`

Weak:
- `@param composition the composition`

### Return values
Use `@return` to describe what is returned and any important meaning.

Good:
- `@return the resolved root action, or an empty {@link Optional} when no actions are discovered`

### Exceptions
Use `@throws` when callers need to understand failure conditions.
Especially document:
- validation failures
- configuration failures
- runtime signaling exceptions
- destructive side effects such as JVM exit

---

## Use of Javadoc Tags

### `{@code ...}`
Use for:
- literals
- property names
- boolean/string values
- short inline code expressions

Examples:
- `{@code true}`
- `{@code paramixel.parallelism}`
- `{@code @Paramixel.ActionFactory}`

### `{@link ...}`
Use for related Paramixel types and methods when cross-reference helps navigation.

Prefer links to:
- related interfaces and implementations
- companion factory methods
- associated result/status types

Do not over-link every sentence.

---

## Nullability and Optional Guidance

Paramixel uses `Optional` in many APIs.
Be explicit about absence semantics.

Prefer wording like:
- “returns an empty `Optional` when no action is discovered”
- “returns the configured executor service, or an empty `Optional` when the context executor will be used”

If null is rejected, document it when helpful via `@throws NullPointerException`.
If blank strings or empty collections are rejected, document that too.

---

## Collections and Ordering

When a method returns or consumes a collection, document:
- whether order matters
- whether the collection is mutable or immutable
- whether null elements are allowed
- whether empty collections are allowed

Examples:
- child actions in declaration order
- immutable validated child list
- shuffled execution order

---

## Execution and Status Semantics

For action- and runner-related APIs, explicitly document:
- when listeners are called
- how pass/fail/skip propagate
- whether execution continues after failure
- whether remaining actions are skipped or still executed
- how parallelism affects scheduling
- whether exit code behavior is influenced by configuration

These contracts are more important than implementation trivia.

---

## Exceptions and Runtime Signaling

For exception types and methods that use them, explain intent clearly.

Examples:
- `FailException` means an action should be reported as failed
- `SkipException` means an action should be reported as skipped
- `ResolverException` indicates invalid or conflicting action-factory discovery
- `ConfigurationException` indicates invalid or unreadable configuration

For helper methods like `fail()` or `skip()`, state that they always throw.

---

## Public API vs SPI vs Support

Be explicit about audience.

### Public API
Document for users of Paramixel.
Focus on usage, contracts, and expected outcomes.

### SPI
Document for advanced integrators.
Clarify extension expectations and any stability caveats.

### Support/internal utilities
Document enough for maintainability, but avoid presenting them as primary user entry points unless that is intentional.

---

## Tone and Style

- Use present tense.
- Prefer active voice.
- Be direct and concrete.
- Keep terminology consistent across files.
- Avoid marketing language.
- Avoid redundant filler.

Prefer:
- “Executes children in order and stops after the first failure.”

Avoid:
- “This amazing class is used to help execute child actions in a very flexible manner.”

---

## Examples of Good Patterns

### Type summary
```java
/**
 * Executes child actions concurrently.
 *
 * <p>Parallel execution uses either an explicitly supplied {@link ExecutorService}
 * or the executor service from the current {@link Context}.
 */
```

### Method with contract
```java
/**
 * Resolves actions matching the supplied selector and composition strategy.
 *
 * @param selector the selector describing which packages or classes should be scanned
 * @param composition the composition strategy for multiple discovered actions
 * @return the resolved root action, or an empty {@link Optional} when no actions are discovered
 */
```

### Exception helper
```java
/**
 * Throws a failure exception with the default message {@code "failed"}.
 *
 * @throws FailException always
 */
```

---

## Things to Avoid

- repeating the identifier without adding meaning
- documenting obvious Java syntax instead of behavior
- vague terms like “handles,” “manages,” or “processes” without specifics
- silently omitting failure behavior on high-impact methods
- overusing links in every sentence
- exposing internal implementation details as if they were API guarantees

---

## Validation Workflow

Before considering Javadoc done:

```bash
./mvnw spotless:apply -pl core
./mvnw -pl core javadoc:javadoc
./mvnw -Pjavadoc-strict -pl core javadoc:javadoc
```

Use the strict profile as the quality gate when the module is ready.

---

## Definition of Professional Javadoc

Javadoc is professional when it:
- explains purpose clearly
- documents contracts precisely
- covers important parameters, return values, and exceptions
- distinguishes API guidance from implementation details
- helps users predict behavior without reading source
- stays consistent across related types
