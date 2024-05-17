# Maintainer Notes

Shell scripts to build and release are located int the `tools` directory.

## Build a pre-release
___

**Pre-release builds are not source controlled (no branch, no tag)**

Command

```shell
./tools/build-and-copy.sh <version> <destination directory>
```

Example

```shell
./tools/build-and-copy.sh 0.20.0-ALPHA-1 "/tmp/"
```

The jars will be located in `/tmp`

## Build and stage 
___

Release builds are source controlled.

- Creates a `release-<version>` branch
- Creates a `<version>` tag
- Pushes the branch and tag to GitHub
- Stages the release to Maven Central

### Step 1

Command

```shell
./tools/build-and-stage.sh <version>
```

Example

```shell
./tools/build-and-stage.sh 0.20.0
```

### Step 2

Download the staged artifacts from Maven Central and run the integration test suite.

```
https://oss.sonatype.org/#stagingRepositories
```

Example

```shell
/home/dhoard/Downloads/jmx_prometheus_javaagent-0.20.0.jar
/home/dhoard/Downloads/jmx_prometheus_httpserver-0.20.0.jar
```

Command

```shell
./tools/patch-and-run-integration-test-suite.sh <javaagent.jar> <httpserver.jar>
```

Example

```shell
./tools/patch-and-run-integration-test-suite.sh /home/dhoard/Downloads/jmx_prometheus_javaagent-0.20.0.jar /home/dhoard/Downloads/jmx_prometheus_httpserver-0.20.0.jar
```

### Step 3

If the integration test suite in Step 2 passes, on Maven Central...

- Click `Close` to trigger Sonatype's verification
- Once closed, click `Release`


### Step 4

Verify the files are available via Maven Central (Maven)

Create a GitHub release

### Step 5

Checkout the `main` branch and increment the version

```shell
git checkout main
./tools/change-version.sh 1.0.0
git add -u
git commit -m "prepare for next development iteration"
```
