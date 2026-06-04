#!/bin/bash

#
# Copyright (C) The Prometheus jmx_exporter Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# Bash script to build, package, and run the tests (single Docker container)
#

set -e
set -o pipefail

usage() {
  echo "Usage: $0 <JAVA DOCKER IMAGE> <PROMETHEUS DOCKER IMAGE> [parallelism] [-D<name>[=<value>] ...]"
  echo "Quote -D values containing shell metacharacters, for example:"
  echo "  $0 <JAVA DOCKER IMAGE> <PROMETHEUS DOCKER IMAGE> [parallelism] '-Dparamixel.match.class=^(?!.*PBKDF).*'"
}

if [[ "$#" -lt 2 ]]; then
  usage
  exit 1
fi

JAVA_DOCKER_IMAGE="$1"
PROMETHEUS_DOCKER_IMAGE="$2"
shift 2

CPU_COUNT="$(nproc)"
PARALLELISM="$CPU_COUNT"
PARALLELISM_SET=false
JAVA_FLAGS=()

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    -D*)
      JAVA_FLAGS+=("$1")
      ;;
    [1-9]*)
      if [[ "$PARALLELISM_SET" == true ]]; then
        usage
        exit 1
      fi
      PARALLELISM="$1"
      PARALLELISM_SET=true
      ;;
    *)
      usage
      exit 1
      ;;
  esac
  shift
done

if ! [[ "$PARALLELISM" =~ ^[1-9][0-9]*$ ]]; then
  echo "Error: parallelism must be an integer greater than 0"
  exit 1
fi

(
  export JAVA_DOCKER_IMAGES="$JAVA_DOCKER_IMAGE"
  export PROMETHEUS_DOCKER_IMAGES="$PROMETHEUS_DOCKER_IMAGE"
  echo "Pulling Docker image ${JAVA_DOCKER_IMAGES} ..."
  docker pull "${JAVA_DOCKER_IMAGES}" > /dev/null 2>&1 || {
    echo "Failed to pull Docker image ${JAVA_DOCKER_IMAGES}"
    exit 1
  }
  echo "Successfully pulled Docker image ${JAVA_DOCKER_IMAGES}"

  echo "Pulling Docker image ${PROMETHEUS_DOCKER_IMAGES} ..."
  docker pull "${PROMETHEUS_DOCKER_IMAGES}" > /dev/null 2>&1 || {
    echo "Failed to pull Docker image ${PROMETHEUS_DOCKER_IMAGES}"
    exit 1
  }
  echo "Successfully pulled Docker image ${PROMETHEUS_DOCKER_IMAGES}"
  ./mvnw clean install "-Dparamixel.parallelism=${PARALLELISM}" "${JAVA_FLAGS[@]}"
) 2>&1 | tee targeted-test.log
