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
# Bash script to generate unique shading prefixes for the exporter and isolator
# artifacts and write them to a Maven properties file.
#
# Each prefix is 8 characters: the first character is alphabetic (so the prefix
# is a valid Java package name segment) and the remaining seven characters are
# alphanumeric. The exporter prefix and the isolator prefix are guaranteed to
# differ, keeping the namespaces of the two artifacts distinct at runtime.
#
# Usage: generate-shade-prefix.sh <output-directory>
#

set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <output-directory>" >&2
  exit 1
fi

OUTPUT_DIRECTORY="$1"
OUTPUT_FILE="${OUTPUT_DIRECTORY}/shade-prefix.properties"

ALPHABET="abcdefghijklmnopqrstuvwxyz"
ALPHANUMERIC="${ALPHABET}0123456789"

generate_prefix() {
  local first="${ALPHABET:$((RANDOM % ${#ALPHABET})):1}"
  local rest=""
  local i
  for ((i = 0; i < 7; i++)); do
    rest+="${ALPHANUMERIC:$((RANDOM % ${#ALPHANUMERIC})):1}"
  done
  printf '%s' "${first}${rest}"
}

EXPORTER_PREFIX="$(generate_prefix)"
ISOLATOR_PREFIX="$(generate_prefix)"
while [ "${EXPORTER_PREFIX}" = "${ISOLATOR_PREFIX}" ]; do
  ISOLATOR_PREFIX="$(generate_prefix)"
done

mkdir -p "${OUTPUT_DIRECTORY}"
cat > "${OUTPUT_FILE}" <<EOF
exporter.shade.prefix=${EXPORTER_PREFIX}
isolator.shade.prefix=${ISOLATOR_PREFIX}
EOF

echo "Generated shading prefixes: exporter=${EXPORTER_PREFIX} isolator=${ISOLATOR_PREFIX} (file: ${OUTPUT_FILE})"
