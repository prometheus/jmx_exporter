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
# Bash script to build, package, and run the regression tests (all Docker containers)
#

set -e
set -o pipefail

usage() {
  echo "Usage: $0 [parallelism] [-D<name>[=<value>] ...]"
  echo "Quote -D values containing shell metacharacters, for example:"
  echo "  $0 [parallelism] '-Dparamixel.match.class=^(?!.*PBKDF).*'"
}

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
  export JAVA_DOCKER_IMAGES=all
  export PROMETHEUS_DOCKER_IMAGES=all
  ./integration_test_suite/pull-docker-images.sh
  ./mvnw clean install "-Dparamixel.parallelism=${PARALLELISM}" "${JAVA_FLAGS[@]}"
) 2>&1 | tee regression-test.log
