# Releasing

## Prerequisites

- `gpg` (for artifact signing)
- `sha256sum` (for checksums)
- `~/.m2/settings.xml` with credentials for Maven Central
- Clean git working directory on `main`/`master` branch

## Usage

```shell
./release.sh <VERSION>

# With specific GPG key
./release.sh <VERSION> --gpg-key <KEY_ID>

# Show help
./release.sh --help
```

## Examples

```shell
# Release version 1.6.0
./release.sh 1.6.0

# Release with specific GPG key
./release.sh 1.6.0 --gpg-key ABC123DEF
```

## What the Release Script Does

1. Validates prerequisites (mvnw, maven settings, gpg, git, sha256sum)
2. Validates git state (clean worktree, on main/master branch)
3. Checks no existing release branch or tag exists
4. Creates release branch: `release-{VERSION}`
5. Updates version across all Maven modules
6. Builds and verifies all modules
7. Deploys `collector` module to Maven Central
8. Assembles release artifacts in `RELEASE/` directory:
   - Copies javaagent, isolator_javaagent, and standalone jars
   - Signs each jar with GPG
   - Generates SHA256 checksums
9. Commits release on release branch
10. Creates annotated tag: `v{VERSION}`
11. Pushes release branch and tag to remote
12. Switches back to main branch
13. Sets post-release version: `{VERSION}-POST`
14. Commits and pushes post-release version

## Release Artifacts

Release artifacts will be located in the `RELEASE` directory:

- `jmx_prometheus_javaagent-<VERSION>.jar`
- `jmx_prometheus_javaagent-<VERSION>.jar.asc`
- `jmx_prometheus_javaagent-<VERSION>.jar.sha256`
- `jmx_prometheus_isolator_javaagent-<VERSION>.jar`
- `jmx_prometheus_isolator_javaagent-<VERSION>.jar.asc`
- `jmx_prometheus_isolator_javaagent-<VERSION>.jar.sha256`
- `jmx_prometheus_standalone-<VERSION>.jar`
- `jmx_prometheus_standalone-<VERSION>.jar.asc`
- `jmx_prometheus_standalone-<VERSION>.jar.sha256`

Attach all files to the GitHub release.