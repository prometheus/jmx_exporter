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

Download the artifacts from the staging repository [https://oss.sonatype.org/#stagingRepositories](https://oss.sonatype.org/#stagingRepositories) and verify them manually:

```sh
# agent
/usr/lib/jvm/java-8-openjdk/bin/java -javaagent:/home/fabian/Downloads/jmx_prometheus_javaagent-0.18.0.jar=12345:./integration_tests/smoke_tests/src/test/resources/config.yml -jar integration_tests/jmx_example_application/target/jmx_example_application.jar
/usr/lib/jvm/java-11-openjdk/bin/java -javaagent:/home/fabian/Downloads/jmx_prometheus_javaagent-0.18.0.jar=12345:./integration_tests/smoke_tests/src/test/resources/config.yml -jar integration_tests/jmx_example_application/target/jmx_example_application.jar
/usr/lib/jvm/java-17-openjdk/bin/java -javaagent:/home/fabian/Downloads/jmx_prometheus_javaagent-0.18.0.jar=12345:./integration_tests/smoke_tests/src/test/resources/config.yml -jar integration_tests/jmx_example_application/target/jmx_example_application.jar
/usr/lib/jvm/jre1.6.0_45/bin/java -javaagent:/home/fabian/Downloads/jmx_prometheus_javaagent_java6-0.18.0.jar=12345:./integration_tests/smoke_tests/src/test/resources/config.yml -jar integration_tests/jmx_example_application/target/jmx_example_application.jar

# standalone
java -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar integration_tests/jmx_example_application/target/jmx_example_application.jar

/usr/lib/jvm/java-8-openjdk/bin/java -jar ~/Downloads/jmx_prometheus_httpserver-0.18.0.jar 9000 ./integration_tests/smoke_tests/src/test/resources/config-httpserver.yml
/usr/lib/jvm/java-11-openjdk/bin/java -jar ~/Downloads/jmx_prometheus_httpserver-0.18.0.jar 9000 ./integration_tests/smoke_tests/src/test/resources/config-httpserver.yml
/usr/lib/jvm/java-17-openjdk/bin/java -jar ~/Downloads/jmx_prometheus_httpserver-0.18.0.jar 9000 ./integration_tests/smoke_tests/src/test/resources/config-httpserver.yml
/usr/lib/jvm/jre1.6.0_45/bin/java -jar ~/Downloads/jmx_prometheus_httpserver_java6-0.18.0.jar 9000 ./integration_tests/smoke_tests/src/test/resources/config-httpserver.yml
```

If everything looks good, click `Close` to trigger Sonatype's verification, then click `Release`.
