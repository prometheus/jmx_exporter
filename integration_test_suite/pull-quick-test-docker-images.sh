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

PWD="$PWD"
function exit_trap() {
  cd "${PWD}" || exit
}
trap exit_trap EXIT
SCRIPT_DIRECTORY=$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)
cd "${SCRIPT_DIRECTORY}" || exit

set -e

function pull_docker_images() {
  while read -r LINE;
  do
    echo "Pulling Docker image ${LINE} ..."
    docker pull "${LINE}" > /dev/null 2>&1 || {
      echo "Failed to pull Docker image ${LINE}"
      exit 1
    }
    echo "Successfully pulled Docker image ${LINE}"
  done < <(grep -v '^#' "${1}")
}

pull_docker_images integration_tests/src/test/resources/quick-test-java-docker-images.txt

pull_docker_images integration_tests/src/test/resources/quick-test-prometheus-docker-images.txt
