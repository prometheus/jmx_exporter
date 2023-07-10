# Maintainer Notes

## Update Dependency Versions

Use the [Versions Maven Plugin](https://www.mojohaus.org/versions-maven-plugin/index.html). Rules are configured in `version-rules.xml`.

```
./mvnw versions:use-next-releases
```

## Release

```
./mvnw release:prepare -DreleaseVersion=0.18.0 -DdevelopmentVersion=0.18.1-SNAPSHOT
./mvnw release:perform -DreleaseVersion=0.18.0 -DdevelopmentVersion=0.18.1-SNAPSHOT
```

`release:prepare` does Github tags and commits, while `release:perform` signs the artifacts and uploads them to the staging repositoring on [https://oss.sonatype.org](https://oss.sonatype.org).

Download the artifacts from the staging repository [https://oss.sonatype.org/#stagingRepositories](https://oss.sonatype.org/#stagingRepositories)

Patch the integration tests resource jars:

```shell
cp jmx_promtheus_httpserver-<version>.jar ./integration_test_suite/integration_tests/src/test/resources/common/jmx_promtheus_httpserver.jar
cp jmx_promtheus_javaagent-<version>.jar ./integration_test_suite/integration_tests/src/test/resources/common/jmx_promtheus_javaagent.jar
```

Run the integration test suite:

```shell
cd integration_test_suite
../mvnw clean verify
```

**Notes**

- The integration tests resource versions of the jars do not contain the version numbers
- You **must** be in the `integration_test_suite` directory

If everything looks good, click `Close` to trigger Sonatype's verification, then click `Release`.

Create a release branch:

```shell
cd <project root>
git checkout -b release-<release version> tags/<release tag>
git push --set-upstream origin release-<release version>
```
