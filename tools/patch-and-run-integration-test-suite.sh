#!/bin/bash

# Function to check exit code
function check_exit_code () {
  if [ ! $? -eq 0 ];
  then
    echo "${1}"
    exit 1
  fi
}

# Function to emit an error message and exit
function emit_error () {
  echo "${1}"
  exit 1;
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

# Change to the integration test suite project
cd integration_test_suite

# Run the integration test suite
../mvnw clean verify
check_exit_code "Integration test suite failed"

# Validate the java agent jar version
DOWNLOAD_JAVA_AGENT_JAR_SHA256=$(sha256sum "${JAVA_AGENT_JAR}")
INTEGRATION_TEST_JAVA_AGENT_JAR_SHA256=$(sha256sum ./integration_tests/src/test/resources/common/jmx_prometheus_javaagent.jar)

if [ "${DOWNLOAD_JAVA_AGENT_JAR_SHA256}" != "${DOWNLOAD_JAVA_AGENT_JAR_SHA256}" ];
then
  emit_error "Java agent jar mismatch"
fi

# Validate the java httpserver jar version
DOWNLOAD_HTTPSERVER_JAR_SHA256=$(sha256sum "${HTTPSERVER_JAR}")
INTEGRATION_HTTPSERVER_JAR_SHA256=$(sha256sum ./integration_tests/src/test/resources/common/jmx_prometheus_httpserver.jar)

if [ "${DOWNLOAD_HTTPSERVER_JAR_SHA256}" != "${DOWNLOAD_HTTPSERVER_JAR_SHA256}" ];
then
  emit_error "Java HTTP server jar mismatch"
fi

# Change to the project root directory
cd "${PROJECT_ROOT_DIRECTORY}"
check_exit_code "Failed to change to project root directory"

echo "------------------------------------------------------------------------"
echo "SUCCESS"
echo "------------------------------------------------------------------------"

