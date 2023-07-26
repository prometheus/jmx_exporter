#!/bin/bash

# Function to check exit code
function check_exit_code () {
  if [ ! $? -eq 0 ];
  then
    echo "${1}"
    exit 1
  fi
}

PROJECT_ROOT_DIRECTORY=$(git rev-parse --show-toplevel)

# Change to the project root directory
cd "${PROJECT_ROOT_DIRECTORY}"
check_exit_code "Failed to change to project root directory"

grep -RiL "Copyright (C) .* The Prometheus jmx_exporter Authors" collector/src/main/java/
check_exit_code "Files are missing copyright"

grep -RiL "Copyright (C) .* The Prometheus jmx_exporter Authors" jmx_prometheus_common/src/main/java/
check_exit_code "Files are missing copyright"

grep -RiL "Copyright (C) .* The Prometheus jmx_exporter Authors" jmx_prometheus_httpserver/src/main/java/
check_exit_code "Files are missing copyright"

grep -RiL "Copyright (C) .* The Prometheus jmx_exporter Authors" jmx_prometheus_javaagent/src/main/java/
check_exit_code "Files are missing copyright"

grep -RiL "Copyright (C) .* The Prometheus jmx_exporter Authors" integration_test_suite/integration_tests/src/main/java/
check_exit_code "Files are missing copyright"

grep -RiL "Copyright (C) .* The Prometheus jmx_exporter Authors" integration_test_suite/integration_tests/src/test/java/
check_exit_code "Files are missing copyright"

grep -RiL "Copyright (C) .* The Prometheus jmx_exporter Authors" integration_test_suite/jmx_example_application/src/main/java/
check_exit_code "Files are missing copyright"
