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

readonly VERSION_PATTERN='^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9._-]+)?$'
readonly BASE_BRANCH_MAIN='main'
readonly BASE_BRANCH_MASTER='master'
readonly POST_SUFFIX='POST'
readonly RELEASE_DIR='RELEASE'
readonly SETTINGS_FILE="${HOME}/.m2/settings.xml"

ARTIFACTS=(
    "jmx_prometheus_javaagent/target/jmx_prometheus_javaagent-{version}.jar"
    "jmx_prometheus_isolator_javaagent/target/jmx_prometheus_isolator_javaagent-{version}.jar"
    "jmx_prometheus_standalone/target/jmx_prometheus_standalone-{version}.jar"
)

gpg_key_id=""
git_remote=""
version=""
original_branch=""

log_info() {
    echo "> $*"
}

log_warn() {
    echo "! $*" >&2
}

log_error() {
    echo "! $*" >&2
}

usage() {
    cat <<EOF
Usage: $(basename "$0") <version> [options]

Arguments:
    version              Release version (e.g., 1.6.0)

Options:
    --gpg-key <key-id>   GPG key ID for signing (uses default key if not specified)
    -h, --help           Show this help message

Examples:
    $(basename "$0") 1.6.0
    $(basename "$0") 1.6.0 --gpg-key ABC123DEF

Requirements:
    - Clean git working directory on main/master branch
    - ~/.m2/settings.xml (for Maven Central deployment)
    - GPG configured for signing
    - Git remote with push access
EOF
}

validate_version() {
    local ver="$1"
    
    if [[ ! "${ver}" =~ ${VERSION_PATTERN} ]]; then
        log_error "Invalid version format: ${ver}"
        log_error "Expected: MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH-label"
        exit 1
    fi
    
    if [[ "${ver}" == *"-${POST_SUFFIX}" ]]; then
        log_error "Version should not end with '-${POST_SUFFIX}' (that's reserved for post-release)"
        exit 1
    fi
}

check_prerequisites() {
    log_info "Checking prerequisites"
    
    if [[ ! -f "mvnw" ]]; then
        log_error "mvnw not found in current directory"
        exit 1
    fi
    
    if [[ ! -f "${SETTINGS_FILE}" ]]; then
        log_error "${SETTINGS_FILE} not found"
        log_error "This file is required for Maven Central deployment"
        exit 1
    fi
    log_info "Found Maven settings: ${SETTINGS_FILE}"
    
    if ! command -v gpg &>/dev/null; then
        log_error "GPG not found - required for artifact signing"
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
    
    if ! command -v git &>/dev/null; then
        log_error "Git not found"
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
    
    log_info "All prerequisites satisfied"
}

check_git_state() {
    log_info "Validating git state"
    
    local status
    status=$(git status --porcelain)
    if [[ -n "${status}" ]]; then
        log_error "Working directory is not clean"
        log_error "Uncommitted changes detected:"
        echo "${status}" | head -20 >&2
        log_error "Commit or stash changes before releasing"
        exit 1
    fi
    log_info "Working directory is clean"
    
    original_branch=$(git rev-parse --abbrev-ref HEAD)
    log_info "Current branch: ${original_branch}"
    
    if [[ "${original_branch}" != "${BASE_BRANCH_MAIN}" && "${original_branch}" != "${BASE_BRANCH_MASTER}" ]]; then
        log_error "Not on main or master branch (currently on ${original_branch})"
        log_error "Releases should be made from main/master"
        exit 1
    fi
    
    git_remote=$(git config --get "branch.${original_branch}.remote" 2>/dev/null || echo "origin")
    log_info "Using git remote: ${git_remote}"
    
    if ! git remote get-url "${git_remote}" &>/dev/null; then
        log_error "Git remote '${git_remote}' not found"
        exit 1
    fi
    
    log_info "Git state validated successfully"
}

check_no_existing_release() {
    local ver="$1"
    local release_branch="release-${ver}"
    local tag_name="v${ver}"
    
    log_info "Checking for existing release"
    
    if git rev-parse --verify "refs/heads/${release_branch}" &>/dev/null; then
        log_error "Release branch already exists: ${release_branch}"
        exit 1
    fi
    
    if git rev-parse --verify "refs/tags/${tag_name}" &>/dev/null; then
        log_error "Tag already exists: ${tag_name}"
        exit 1
    fi
    
    if git ls-remote --heads "${git_remote}" "refs/heads/${release_branch}" 2>/dev/null | grep -q .; then
        log_error "Remote release branch already exists: ${release_branch}"
        exit 1
    fi
    
    if git ls-remote --tags "${git_remote}" "refs/tags/${tag_name}" 2>/dev/null | grep -q .; then
        log_error "Remote tag already exists: ${tag_name}"
        exit 1
    fi
    
    log_info "No existing release found for version ${ver}"
}

set_version() {
    local ver="$1"
    
    log_info "Setting version to ${ver}"
    
    ./mvnw -B versions:set -DnewVersion="${ver}" -DprocessAllModules -DgenerateBackupPoms=false
    
    find . -name "*.versionsBackup" -delete 2>/dev/null || true
    
    log_info "Version updated to ${ver}"
}

build_and_verify() {
    log_info "Building and verifying"
    
    ./mvnw -B clean verify
    
    log_info "Build and verification completed"
}

deploy_collector() {
    log_info "Deploying collector to Maven Central"
    
    ./mvnw -s "${SETTINGS_FILE}" -pl collector -P release clean deploy
    
    log_info "Collector deployed to Maven Central"
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
    log_info "Release artifacts:"
    ls -l "${RELEASE_DIR}/" | tail -n +2 | awk '{$1=$2=$3=$4=""; sub(/^[ \t]+/, ""); print}' | column -t | sed 's/^/>  /'
}

create_release_branch() {
    local ver="$1"
    local release_branch="release-${ver}"
    
    log_info "Creating release branch"
    
    git checkout -b "${release_branch}"
    
    log_info "Created branch: ${release_branch}"
}

commit_release() {
    local ver="$1"
    
    log_info "Committing release"
    
    git add -u
    git commit -s -m "Release ${ver}"
    
    log_info "Committed version change"
}

tag_release() {
    local ver="$1"
    local tag_name="v${ver}"
    
    log_info "Creating git tag"
    
    git tag -a "${tag_name}" -m "Release ${ver}"
    
    log_info "Created tag: ${tag_name}"
}

push_release() {
    local ver="$1"
    local release_branch="release-${ver}"
    local tag_name="v${ver}"
    
    log_info "Pushing release to remote"
    
    git push "${git_remote}" "${release_branch}"
    log_info "Pushed branch: ${release_branch}"
    
    git push "${git_remote}" "${tag_name}"
    log_info "Pushed tag: ${tag_name}"
}

post_release_version() {
    local ver="$1"
    local post_version="${ver}-${POST_SUFFIX}"
    
    log_info "Setting post-release version"
    
    git checkout "${original_branch}"
    log_info "Switched to branch: ${original_branch}"
    
    set_version "${post_version}"
    
    git add -u
    git commit -s -m "Prepare for development"
    
    log_info "Post-release version set to ${post_version}"
}

push_post_release() {
    log_info "Pushing post-release"
    
    git push "${git_remote}" "${original_branch}"
    
    log_info "Pushed ${original_branch} to ${git_remote}"
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
                if [[ -z "${version}" ]]; then
                    version="$1"
                else
                    log_error "Multiple versions specified"
                    usage
                    exit 1
                fi
                shift
                ;;
        esac
    done
    
    if [[ -z "${version}" ]]; then
        log_error "Version argument is required"
        usage
        exit 1
    fi
    
    echo "> Prometheus JMX Exporter Release"
    log_info "Release version: ${version}"
    log_info "Git remote: ${git_remote:-auto-detect}"
    log_info "GPG key: ${gpg_key_id:-default}"
    
    validate_version "${version}"
    check_prerequisites
    check_git_state
    check_no_existing_release "${version}"
    
    echo ""
    log_info "Ready to release version ${version}"
    echo ""
    log_info "This will:"
    log_info "  - Create branch: release-${version}"
    log_info "  - Build and deploy to Maven Central"
    log_info "  - Create git tag: v${version}"
    log_info "  - Push to remote: ${git_remote}"
    log_info "  - Sign artifacts with GPG key: ${gpg_key_id:-default}"
    echo ""
    
    read -r -p "Proceed with release? [y/N] " response
    case "${response}" in
        [yY][eE][sS]|[yY])
            log_info "Proceeding with release"
            ;;
        *)
            log_error "Release cancelled"
            exit 1
            ;;
    esac
    echo ""
    
    create_release_branch "${version}"
    set_version "${version}"
    build_and_verify
    deploy_collector
    assemble_artifacts "${version}"
    commit_release "${version}"
    tag_release "${version}"
    push_release "${version}"
    post_release_version "${version}"
    push_post_release
    
    log_info "Release ${version} completed successfully!"
}

main "$@"