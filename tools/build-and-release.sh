#!/bin/bash

# Function to check exit code
function check_exit_code () {
  if [ ! $? ];
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

# Usage
if [ "$#" -ne 1 ];
then
  echo "Usage: ${0} <version>"
  exit 1
fi

VERSION="${1}"
PROJECT_ROOT_DIRECTORY=$(git rev-parse --show-toplevel)

# Change to the project root directory
cd "${PROJECT_ROOT_DIRECTORY}"

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

cd "${PROJECT_ROOT_DIRECTORY}"
check_exit_code "Failed to change to project root directory"

# Check for missing copyright notices
tools/copyright-check.sh
check_exit_code "Copyright check failed"

# Apply Google Java format
#tools/apply-google-java-format.sh

# Verify the code builds
./mvnw -P release clean verify
check_exit_code "Maven build failed"

# Checkout a release branch
git checkout -b "release-${VERSION}"
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

# Build and deploy
./mvnw -P release clean deploy
check_exit_code "Maven deploy [${VERSION}] failed"

# Push the branch
git push --set-upstream origin release-"${VERSION}"
check_exit_code "Git push [${VERSION}] failed"

# Tag the version
git tag "${VERSION}"
check_exit_code "Git tag [${VERSION}] failed"

# Push the tag
git push origin "${VERSION}"
check_exit_code "Git tag [${VERSION}] push failed"

# Checkout the main branch
git checkout main
check_exit_code "Git checkout [main] failed"

echo "------------------------------------------------------------------------"
echo "SUCCESS"
echo "------------------------------------------------------------------------"
