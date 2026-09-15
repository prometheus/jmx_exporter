#!/usr/bin/env bash

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
# Build the JMX exporter release artifacts and assemble the RELEASE/ directory
# so the assets can be inspected locally or attached to a GitHub release manually.
#
# This is a packaging-only helper. Unlike release.sh it does NOT bump versions,
# touch git, or deploy to Maven Central, and it is intentionally independent of
# release.sh. It takes no arguments.
#
# Usage: ./scripts/package.sh
#

set -euo pipefail

# Resolve the repository root from this script's location (this file lives in scripts/).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

readonly RELEASE_DIR='RELEASE'

log_info() {
    echo "> $*"
}

log_error() {
    echo "! $*" >&2
}

# Release jars assembled into RELEASE/.
JARS=(
    "jmx_prometheus_javaagent/target/jmx_prometheus_javaagent-{version}.jar"
    "jmx_prometheus_isolator_javaagent/target/jmx_prometheus_isolator_javaagent-{version}.jar"
    "jmx_prometheus_standalone/target/jmx_prometheus_standalone-{version}.jar"
)

# CycloneDX SBOMs, one per published artifact. Each entry is "<source path>|<release file name>".
SBOMS=(
    "collector/target/bom.json|collector-{version}.cdx.json"
    "jmx_prometheus_javaagent/target/bom.json|jmx_prometheus_javaagent-{version}.cdx.json"
    "jmx_prometheus_isolator_javaagent/target/bom.json|jmx_prometheus_isolator_javaagent-{version}.cdx.json"
    "jmx_prometheus_standalone/target/bom.json|jmx_prometheus_standalone-{version}.cdx.json"
)

build() {
    log_info "Building artifacts (tests skipped)"
    ./mvnw -B clean package \
        -pl collector,jmx_prometheus_common,jmx_prometheus_javaagent,jmx_prometheus_isolator_javaagent,jmx_prometheus_standalone \
        -DskipTests
}

# Print the current project version as declared in the POMs.
resolve_version() {
    local ver
    ver="$(./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout -pl collector 2>/dev/null | tail -n 1)"
    if [[ -z "${ver}" ]]; then
        log_error "Failed to resolve project version"
        exit 1
    fi
    printf '%s' "${ver}"
}

assemble() {
    local ver="$1"

    log_info "Assembling ${RELEASE_DIR}/"
    rm -rf "${RELEASE_DIR}"
    mkdir -p "${RELEASE_DIR}"

    local path
    for template in "${JARS[@]}"; do
        path="${template//\{version\}/${ver}}"
        if [[ ! -f "${path}" ]]; then
            log_error "Artifact not found: ${path}"
            exit 1
        fi
        cp "${path}" "${RELEASE_DIR}/"
        log_info "Copied: $(basename "${path}")"
    done

    local entry src dst
    for entry in "${SBOMS[@]}"; do
        src="${entry%%|*}"
        dst="${entry##*|}"
        dst="${dst//\{version\}/${ver}}"
        if [[ ! -f "${src}" ]]; then
            log_error "SBOM not found: ${src}"
            exit 1
        fi
        cp "${src}" "${RELEASE_DIR}/${dst}"
        log_info "Copied SBOM: ${dst}"
    done

    pushd "${RELEASE_DIR}" >/dev/null

    local filename
    for filename in *.jar *.cdx.json; do
        if [[ -f "${filename}" ]]; then
            gpg --armor --detach-sign "${filename}"
            log_info "Signed: ${filename}.asc"

            sha256sum "${filename}" > "${filename}.sha256"
            log_info "Checksum: ${filename}.sha256"
        fi
    done

    popd >/dev/null
}

main() {
    build

    local ver
    ver="$(resolve_version)"
    log_info "Version: ${ver}"

    assemble "${ver}"

    log_info "Done. Release assets are in ${RELEASE_DIR}/:"
    ls -1 "${RELEASE_DIR}/" | sed 's/^/  /'
}

main
