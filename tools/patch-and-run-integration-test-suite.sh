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

# Change to the project root directory
cd "${PROJECT_ROOT_DIRECTORY}"
check_exit_code "Failed to change to project root directory"

if [ "$#" -ne 2 ];
then
  echo "Usage: ${0} <javaagent.jar> <httpserver.jar>"
  exit 1
fi

rm -Rf ./integration_test_suite/integration_tests/src/test/resources/common/* > /dev/null 2>&1

cp "${JAVA_AGENT_JAR}" ./integration_test_suite/integration_tests/src/test/resources/common/jmx_prometheus_javaagent.jar
check_exit_code "Failed to patch the integration test suite [${JAVA_AGENT_JAR}]"

cp "${HTTPSERVER_JAR}" ./integration_test_suite/integration_tests/src/test/resources/common/jmx_prometheus_httpserver.jar
check_exit_code "Failed to patch the integration test suite [${HTTPSERVER_JAR}]"

cd ./integration_test_suite
../mvnw clean verify
check_exit_code "Integration test suite failed"

cd "${PROJECT_ROOT_DIRECTORY}"
check_exit_code "Failed to change to project root directory"

echo "------------------------------------------------------------------------"
echo "SUCCESS"
echo "------------------------------------------------------------------------"

