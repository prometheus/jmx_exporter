#!/bin/bash

function exit_trap() {
  cd "${PWD}"
}
trap exit_trap EXIT
SCRIPT_DIRECTORY=$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)
cd "${SCRIPT_DIRECTORY}"

set -e

./pull-java-docker-images.sh
./pull-prometheus-docker-images.sh