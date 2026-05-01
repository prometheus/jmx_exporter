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

if [[ "$#" -lt 2 || "$#" -gt 3 ]]; then
    echo "Usage: $0 <JAVA DOCKER IMAGE> <PROMETHEUS DOCKER IMAGE> [parallelism]"
    exit 1
fi

CPU_COUNT="$(nproc)"
PARALLELISM="${3:-$CPU_COUNT}"

if ! [[ "$PARALLELISM" =~ ^[1-9][0-9]*$ ]]; then
  echo "Error: parallelism must be an integer greater than 0"
  exit 1
fi

(
  export JAVA_DOCKER_IMAGES="$1"
  export PROMETHEUS_DOCKER_IMAGES="$2"
  docker pull "$JAVA_DOCKER_IMAGES"
  docker pull "$PROMETHEUS_DOCKER_IMAGES"
  ./mvnw clean install "-Dparamixel.parallelism=${PARALLELISM}"
) 2>&1 | tee targeted-test.log
