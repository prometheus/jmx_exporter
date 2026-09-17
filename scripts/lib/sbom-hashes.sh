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
# Shared CycloneDX SBOM artifact-hash helpers.
#
# Sourced by release.sh and scripts/package.sh so release SBOMs are bound to the
# exact published artifacts in a single place. This file is not executable; it
# only defines the shared constants and functions.
#

# CycloneDX hash algorithms recorded in each release JAR's SBOM
# metadata.component.hashes entry. Each entry is "<CycloneDX alg>:<openssl digest>".
# These mirror the algorithms CycloneDX records for the dependency components.
HASH_ALGORITHMS=(
    "MD5:md5"
    "SHA-1:sha1"
    "SHA-256:sha256"
    "SHA-384:sha384"
    "SHA-512:sha512"
    "SHA3-256:sha3-256"
    "SHA3-384:sha3-384"
    "SHA3-512:sha3-512"
)

# Verify the tools required by add_artifact_hash_to_sbom are available.
require_sbom_hash_commands() {
    local command

    for command in openssl jq awk; do
        if ! command -v "${command}" >/dev/null 2>&1; then
            log_error "Required command not found: ${command}"
            exit 1
        fi
    done
}

# Add all supported CycloneDX hashes of the final release JAR to the top-level
# metadata.component.hashes entry so the SBOM can be tied to the exact artifact.
add_artifact_hash_to_sbom() {
    local jar="$1"
    local sbom="$2"

    if [[ ! -f "${jar}" ]]; then
        log_error "Artifact not found: ${jar}"
        exit 1
    fi

    if [[ ! -f "${sbom}" ]]; then
        log_error "SBOM not found: ${sbom}"
        exit 1
    fi

    local hashes='[]'
    local entry alg openssl_alg digest

    for entry in "${HASH_ALGORITHMS[@]}"; do
        alg="${entry%%:*}"
        openssl_alg="${entry#*:}"
        if ! digest="$(openssl dgst -r "-${openssl_alg}" "${jar}" | awk '{print $1}')"; then
            log_error "Failed to compute ${alg} for ${jar}"
            exit 1
        fi
        hashes="$(jq -c --arg alg "${alg}" --arg content "${digest}" '. + [{alg: $alg, content: $content}]' <<<"${hashes}")"
    done

    local tmp
    tmp="$(mktemp)"

    if ! jq \
        --argjson hashes "${hashes}" \
        '.metadata.component.hashes = $hashes' \
        "${sbom}" > "${tmp}"; then
        rm -f "${tmp}"
        log_error "Failed to update SBOM: ${sbom}"
        exit 1
    fi

    mv "${tmp}" "${sbom}"

    log_info "Added ${#HASH_ALGORITHMS[@]} artifact hashes to SBOM: $(basename "${sbom}")"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "This file is intended to be sourced, not executed." >&2
    exit 1
fi
