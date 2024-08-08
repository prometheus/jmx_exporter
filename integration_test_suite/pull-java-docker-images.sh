#!/bin/bash

PWD="$PWD"
function exit_trap() {
  cd "${PWD}"
  echo $?
}
trap exit_trap EXIT
SCRIPT_DIRECTORY=$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)
cd "${SCRIPT_DIRECTORY}"

function check_exit_code() {
  if [ "$?" != "0" ];
  then
    echo "Failed to pull Docker image ${1}";
    exit $?
  fi
}

grep -v '^#' integration_tests/src/test/resources/java-docker-images.txt | while read -r LINE;
do
  docker pull "${LINE}"
  check_exit_code "${LINE}"
done
