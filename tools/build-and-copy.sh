#!/bin/bash

# Function to check exit code
function check_exit_code () {
  if [ ! $? -eq 0 ];
  then
    echo "${1}"
    exit 1
  fi
}

if [ "$#" -ne 2 ];
then
  echo "Usage: ${0} <version> <destination directory>"
  exit 1
fi

VERSION="${1}"
DESTINATION_DIRECTORY="${2}"
PROJECT_ROOT_DIRECTORY=$(git rev-parse --show-toplevel)

# Check for uncommitted changes
git diff --quiet HEAD
if [ ! $? -eq 0 ];
then
  echo "------------------------------------------------------------------------"
  echo "UNCOMMITTED CHANGES"
  echo "------------------------------------------------------------------------"
  echo ""
  git status
  exit 1
fi

# Check for any uncommitted changes
git diff --quiet HEAD
if [ ! $? -eq 0 ];
then
  echo "------------------------------------------------------------------------"
  echo "UNCOMMITTED CHANGES"
  echo "------------------------------------------------------------------------"
  echo ""
  git status
  exit 1
fi

# Check if the destination directory exists
if [ ! -d "${DESTINATION_DIRECTORY}" ];
then
  # Create the destination directory
  mkdir -p "${DESTINATION_DIRECTORY}"
  check_exit_code "Failed to create destination directory [${DESTINATION_DIRECTORY}]"
fi

# Change to the project root directory
cd "${PROJECT_ROOT_DIRECTORY}"
check_exit_code "Failed to change to project root directory"

# Check for missing copyright notices
tools/copyright-check.sh
check_exit_code "Copyright check failed"

# Verify the code builds
./mvnw clean verify
check_exit_code "Maven build failed"

# Delete any previous build branch
git branch -D "build-${VERSION}" > /dev/null 2>&1

# Checkout a build branch
git checkout -b "build-${VERSION}"
check_exit_code "Git checkout [${VERSION}] failed"

# Update the build versions
mvn versions:set -DnewVersion="${VERSION}" -DprocessAllModules
check_exit_code "Maven update versions [${VERSION}] failed"
rm -Rf $(find . -name "*versionsBackup")

# Add changed files
git add -u
check_exit_code "Git add failed"

# Commit the changed files
git commit -m "${VERSION}"
check_exit_code "Git commit failed"

# Verify the code builds
./mvnw clean verify
check_exit_code "Maven build [${VERSION}] failed"

# Copy the httpserver jar
cp jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-"${VERSION}".jar "${DESTINATION_DIRECTORY}"/
check_exit_code "Failed to copy jmx_prometheus_httpserver-"${VERSION}".jar to ${DESTINATION_DIRECTORY}"

# Copy the javaagent jar
cp jmx_prometheus_javaagent/target/jmx_prometheus_javaagent-"${VERSION}".jar "${DESTINATION_DIRECTORY}"/
check_exit_code "Failed to copy jmx_prometheus_javaagent-"${VERSION}".jar to ${DESTINATION_DIRECTORY}"

# Reset the branch
git reset --hard HEAD
check_exit_code "Git reset hard failed"

# Checkout the main branch
git checkout main
check_exit_code "Git checkout [main] failed"

# Delete the build branch
git branch -D "build-${VERSION}"
check_exit_code "Git delete branch [build-${VERSION}] failed"

echo "------------------------------------------------------------------------"
echo "SUCCESS"
echo "------------------------------------------------------------------------"
