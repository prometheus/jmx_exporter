#!/bin/bash

# Function to check exit code
function check_exit_code () {
  if [ ! $? -eq 0 ];
  then
    echo "${1}"
    exit 1
  fi
}

JAVA_AGENT_JAR="${1}"
HTTPSERVER_JAR="${2}"
PROJECT_ROOT_DIRECTORY=$(git rev-parse --show-toplevel)

if [ "$#" -ne 2 ];
then
  echo "Usage: ${0} <javaagent.jar> <httpserver.jar>"
  exit 1
fi

# Change to the project root directory
cd "${PROJECT_ROOT_DIRECTORY}"
check_exit_code "Failed to change to project root directory"

# Remove current exporter jars
rm -Rf ./integration_test_suite/integration_tests/src/test/resources/common/* > /dev/null 2>&1

# Copy the Java agent exporter jar into the integration test suite
cp "${JAVA_AGENT_JAR}" ./integration_test_suite/integration_tests/src/test/resources/common/jmx_prometheus_javaagent.jar
check_exit_code "Failed to patch the integration test suite [${JAVA_AGENT_JAR}]"

# Copy the Java HTTP server exporter jar into the integration test suite
cp "${HTTPSERVER_JAR}" ./integration_test_suite/integration_tests/src/test/resources/common/jmx_prometheus_httpserver.jar
check_exit_code "Failed to patch the integration test suite [${HTTPSERVER_JAR}]"

# Pull smoke test Docker images
./integration_test_suite/docker-pull-images.smoke-test.sh
check_exit_code "Failed to update smoke test Docker images"

./mvnw clean verify
check_exit_code "Integration test suite failed"

echo "------------------------------------------------------------------------"
echo "SUCCESS"
echo "------------------------------------------------------------------------"

