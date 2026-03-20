# Maintainer Notes

## Release

The `release.py` script performs a complete release workflow:
- Version updates across all Maven modules
- Building and verification
- Artifact deployment to Maven Central
- Creating release artifacts with GPG signatures and checksums
- Git tagging and branch management
- Post-release version bump

**Prerequisites**

- Python 3.6+
- `gpg` (for artifact signing)
- `sha256sum` (for checksums)
- `~/.m2/prometheus.settings.xml` (for Maven Central deployment)
- Clean git working directory on `main`/`master` branch

**Usage**

```shell
# Dry-run mode (default, no changes made)
./release.py <VERSION>

# Execute release
./release.py <VERSION> --execute

# With custom GPG key
./release.py <VERSION> --execute --gpg-key <KEY_ID>

# Verbose output
./release.py <VERSION> --execute --verbose

# Resume from specific step (if interrupted)
./release.py <VERSION> --execute --resume <STEP>
```

**Available Steps for --resume**

- `check_prerequisites`
- `validate_git`
- `update_version`
- `build_verify`
- `deploy`
- `assemble_artifacts`
- `git_release`
- `post_release`

**Examples**

```shell
# Preview release 1.6.0 (dry-run)
./release.py 1.6.0

# Execute release 1.6.0
./release.py 1.6.0 --execute

# Resume from deploy step (after interruption)
./release.py 1.6.0 --execute --resume deploy
```

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