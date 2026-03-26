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

set -euo pipefail

readonly RELEASE_DIR='RELEASE'

ARTIFACTS=(
    "jmx_prometheus_javaagent/target/jmx_prometheus_javaagent-{version}.jar"
    "jmx_prometheus_isolator_javaagent/target/jmx_prometheus_isolator_javaagent-{version}.jar"
    "jmx_prometheus_standalone/target/jmx_prometheus_standalone-{version}.jar"
)

gpg_key_id=""

log_info() {
    echo "> $*"
}

log_error() {
    echo "! $*" >&2
}

usage() {
    cat <<EOF
Usage: $(basename "$0") [options]

Options:
    --gpg-key <key-id>   GPG key ID for signing (uses default key if not specified)
    -h, --help           Show this help message

Description:
    Build the JMX Exporter and create release artifacts in the RELEASE/ directory.
    Version is automatically detected from pom.xml.

Examples:
    $(basename "$0")
    $(basename "$0") --gpg-key ABC123DEF

Requirements:
    - mvnw in current directory
    - gpg (for artifact signing)
    - sha256sum (for checksums)
EOF
}

check_prerequisites() {
    log_info "Checking prerequisites"
    
    if [[ ! -f "mvnw" ]]; then
        log_error "mvnw not found in current directory"
        exit 1
    fi
    
    if ! command -v gpg &>/dev/null; then
        log_error "GPG not found - required for artifact signing"
        exit 1
    fi
    
    if ! command -v sha256sum &>/dev/null; then
        log_error "sha256sum not found"
        exit 1
    fi
    
    if ! command -v sed &>/dev/null; then
        log_error "sed not found"
        exit 1
    fi
    
    if ! command -v awk &>/dev/null; then
        log_error "awk not found"
        exit 1
    fi
    
    if ! command -v column &>/dev/null; then
        log_error "column not found"
        exit 1
    fi
    
    if [[ -n "${gpg_key_id}" ]]; then
        if ! gpg --list-secret-keys "${gpg_key_id}" &>/dev/null; then
            log_error "GPG secret key not found: ${gpg_key_id}"
            exit 1
        fi
        log_info "Using GPG key: ${gpg_key_id}"
    else
        log_info "Using default GPG key"
    fi
    
    log_info "All prerequisites satisfied"
}

get_version() {
    local version
    
    version=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null | head -n 1)
    
    if [[ -z "${version}" || "${version}" == *"@"* ]]; then
        log_error "Could not determine version from pom.xml"
        exit 1
    fi
    
    echo "${version}"
}

build_and_verify() {
    log_info "Building and verifying"
    
    ./mvnw -B clean verify
    
    log_info "Build and verification completed"
}

assemble_artifacts() {
    local ver="$1"
    
    log_info "Assembling release artifacts"
    
    rm -rf "${RELEASE_DIR}"
    mkdir -p "${RELEASE_DIR}"
    
    local artifact_path
    for template in "${ARTIFACTS[@]}"; do
        artifact_path="${template//\{version\}/${ver}}"
        if [[ ! -f "${artifact_path}" ]]; then
            log_error "Artifact not found: ${artifact_path}"
            exit 1
        fi
        cp "${artifact_path}" "${RELEASE_DIR}/"
        log_info "Copied: ${artifact_path}"
    done
    
    log_info "Artifacts copied to ${RELEASE_DIR}/"
    
    pushd "${RELEASE_DIR}" >/dev/null
    
    local gpg_opts=("gpg")
    if [[ -n "${gpg_key_id}" ]]; then
        gpg_opts+=("--default-key" "${gpg_key_id}")
    fi
    
    local filename
    for filename in *.jar; do
        if [[ -f "${filename}" ]]; then
            "${gpg_opts[@]}" --armor --detach-sign "${filename}"
            log_info "Signed: ${filename}.asc"
            
            sha256sum "${filename}" > "${filename}.sha256"
            log_info "Checksum: ${filename}.sha256"
        fi
    done
    
    popd >/dev/null
    
    log_info "Signatures and checksums generated"
    log_info "Build artifacts:"
    ls -l "${RELEASE_DIR}/" | tail -n +2 | awk '{$1=$2=$3=$4=""; sub(/^[ \t]+/, ""); print}' | column -t | sed 's/^/>  /'
}

main() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --gpg-key)
                gpg_key_id="$2"
                shift 2
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            -*)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
            *)
                log_error "Unexpected argument: $1"
                usage
                exit 1
                ;;
        esac
    done
    
    echo "> Prometheus JMX Exporter Build"
    log_info "GPG key: ${gpg_key_id:-default}"
    
    mkdir -p "${RELEASE_DIR}"
    rm -f "${RELEASE_DIR}"/* 2>/dev/null || true
    
    check_prerequisites
    
    local version
    version=$(get_version)
    log_info "Version: ${version}"
    
    build_and_verify
    assemble_artifacts "${version}"
    
    log_info "Build completed successfully!"
}

main "$@"