# Maintainer Notes

## Release

Verifyica [Pipeliner](https://github.com/verifyica-team/pipeliner) is used to run the release pipeline.

- builds & runs integration tests using smoke test containers
- creates & copies release artifacts
- generates signatures and checksums for release artifacts
- creates the release branch
- tags the release
- updates the `main` branch for development

**Notes**

- `gpg` is required
- `sha256sum` is required

### Example:

```shell
./pipeliner -Prelease=<RELEASE VERSION> release.yaml
```

### Concrete Example:

```shell
./pipeliner -Prelease=1.1.0 release.yaml
```

## Release Artifacts

Release artifacts will be located in the `RELEASE` directory.

Attach all files to the GitHub release.
