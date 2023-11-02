#!/bin/bash

# Function to check exit code
function check_exit_code () {
  if [ ! $? -eq 0 ];
  then
    echo "------------------------------------------------------------------------"
    echo "${1}"
    echo "------------------------------------------------------------------------"
    exit 1
  fi
}

# Usage
if [ "$#" -ne 2 ];
then
  echo "Usage: ${0} <version> <destination directory>"
  exit 1
fi

PROJECT_ROOT_DIRECTORY=$(git rev-parse --show-toplevel)

# Change to the project root directory
cd "${PROJECT_ROOT_DIRECTORY}"
check_exit_code "Failed to change to project root directory"

# Get the current version
CURRENT_VERSION=$(./mvnw -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)
BUILD_VERSION="${1}"
DESTINATION_DIRECTORY="${2}"

# Check / create the destination directory
if [ ! -d "${DESTINATION_DIRECTORY}" ];
then
  mkdir -p "${DESTINATION_DIRECTORY}"
  check_exit_code "Creating destination directory [${DESTINATION_DIRECTORY}] failed"
fi

# Update the versions
./mvnw versions:set -DnewVersion="${BUILD_VERSION}" -DprocessAllModules >> /dev/null
check_exit_code "Maven update versions [${BUILD_VERSION}] failed"
rm -Rf $(find . -name "*versionsBackup")

# Build and package the exporter jars
./mvnw clean package
BUILD_EXIT_CODE="$?"

# Copy the exporter jars to the destination directory
if [ "${BUILD_EXIT_CODE}" == "0" ];
then
  cp "jmx_prometheus_javaagent/target/jmx_prometheus_javaagent-${BUILD_VERSION}.jar" "${DESTINATION_DIRECTORY}"
  cp "jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-${BUILD_VERSION}.jar" "${DESTINATION_DIRECTORY}"
fi

# Revert the versions
./mvnw versions:set -DnewVersion="${CURRENT_VERSION}" -DprocessAllModules >> /dev/null
check_exit_code "Maven update versions [${CURRENT_VERSION}] failed"
rm -Rf $(find . -name "*versionsBackup")

echo "------------------------------------------------------------------------"
if [ "${BUILD_EXIT_CODE}" == "0" ];
then
    echo "SUCCESS"
else
    echo "FAILURE"
fi
echo "------------------------------------------------------------------------"
