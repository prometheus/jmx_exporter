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
  echo $?
}
trap exit_trap EXIT
SCRIPT_DIRECTORY=$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)
cd "${SCRIPT_DIRECTORY}" || exit

function check_exit_code() {
  if [ "$?" != "0" ];
  then
    echo "Failed to execute ${1}";
    exit 1
  fi
}

./pull-smoke-test-java-docker-images.sh
check_exit_code "./pull-smoke-test-java-docker-images.sh"

./pull-smoke-test-prometheus-docker-images.sh
check_exit_code "./pull-smoke-test-prometheus-docker-images.sh"
