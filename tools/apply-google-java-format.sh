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

# Change to the project root directory
PROJECT_ROOT_DIRECTORY=$(git rev-parse --show-toplevel)

cd "${PROJECT_ROOT_DIRECTORY}"
check_exit_code "Failed to change to project root directory"

# Find all Java files
find . -type f | grep ".java$" | grep -v ".mvn" > .files.tmp

# Process this list of Java files
while read FILE;
do
  java -jar tools/google-java-format-1.17.0-all-deps.jar --aosp -r "${FILE}"
done < .files.tmp

# Remove the list of files
rm -Rf .files.tmp
