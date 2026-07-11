#!/usr/bin/env bash
#
# Copyright (c) 2026-present Douglas Hoard
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR
readonly PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly WEBSITE_DIR="${PROJECT_DIR}/website"
readonly VERSIONS_FILE="${WEBSITE_DIR}/versions.json"
readonly VERSIONED_DOCS_DIR="${WEBSITE_DIR}/versioned_docs"
readonly VERSIONED_SIDEBARS_DIR="${WEBSITE_DIR}/versioned_sidebars"
readonly SIDEBARS_FILE="${WEBSITE_DIR}/sidebars.js"

log() {
    echo "[INFO] $*"
}

warn() {
    echo "[WARN] $*" >&2
}

fail() {
    echo "[ERROR] $*" >&2
    exit 1
}

check_dependencies() {
    if ! command -v node &>/dev/null; then
        fail "node is not installed. Please install Node.js 20+."
    fi

    if ! command -v npm &>/dev/null; then
        fail "npm is not installed. Please install Node.js 20+."
    fi

    if ! command -v awk &>/dev/null; then
        fail "awk is not installed. Please install awk."
    fi
}

get_latest_version() {
    [[ -f "${VERSIONS_FILE}" ]] || fail "Versions file not found: ${VERSIONS_FILE}"

    (cd "${WEBSITE_DIR}" && node -e "
        const versions = require('./versions.json');
        if (!Array.isArray(versions) || versions.length === 0) {
            process.exit(1);
        }
        console.log(versions[0]);
    ") || fail "Failed to read latest version from ${VERSIONS_FILE}"
}

resolve_doc_id() {
    local docs_root="$1"
    local file="$2"
    local frontmatter_id

    frontmatter_id=$(awk '
        /^---$/ { in_fm = (in_fm ? 0 : 1); next }
        in_fm && /^id:/ {
            sub(/^id:[[:space:]]*/, "")
            print
            exit
        }
    ' "$file" 2>/dev/null)

    if [[ -n "${frontmatter_id:-}" ]]; then
        echo "$frontmatter_id"
        return
    fi

    local relative_path
    relative_path="${file#${docs_root}/}"
    relative_path="${relative_path%.md}"
    relative_path="${relative_path%.mdx}"
    echo "$relative_path"
}

get_all_doc_ids() {
    local docs_root="$1"
    local file

    [[ -d "${docs_root}" ]] || fail "Documentation directory not found: ${docs_root}"

    while IFS= read -r -d '' file; do
        resolve_doc_id "${docs_root}" "$file"
    done < <(find "${docs_root}" -type f \( -name "*.md" -o -name "*.mdx" \) -print0) | sort -u
}

get_sidebar_doc_ids() {
    local sidebar_file="$1"

    [[ -f "${sidebar_file}" ]] || fail "Sidebar file not found: ${sidebar_file}"

    (cd "${WEBSITE_DIR}" && SIDEBAR_FILE="${sidebar_file}" node <<'NODE'
const path = require('path');
const sidebarFile = process.env.SIDEBAR_FILE;
const sidebars = require(path.resolve(sidebarFile));
const ids = [];

function extractIds(items) {
  for (const item of items) {
    if (typeof item === 'string') {
      ids.push(item);
    } else if (item && typeof item === 'object' && item.items) {
      extractIds(item.items);
    }
  }
}

for (const sidebar of Object.values(sidebars)) {
  extractIds(sidebar);
}

console.log(ids.sort().join('\n'));
NODE
    ) || fail "Failed to parse sidebar file: ${sidebar_file}"
}

validate_sidebar_file() {
    local sidebar_file="$1"
    local docs_root="$2"
    local label="$3"

    local sidebar_ids actual_ids missing_ids
    sidebar_ids=$(get_sidebar_doc_ids "${sidebar_file}")
    actual_ids=$(get_all_doc_ids "${docs_root}")
    missing_ids=""

    local id
    while IFS= read -r id; do
        [[ -z "$id" ]] && continue
        if ! echo "$actual_ids" | grep -qx "$id"; then
            if [[ -n "$missing_ids" ]]; then
                missing_ids="${missing_ids}"$'\n'"  - $id"
            else
                missing_ids="  - $id"
            fi
        fi
    done <<< "$sidebar_ids"

    if [[ -n "$missing_ids" ]]; then
        fail "${label} sidebar references non-existent document IDs:${missing_ids}"$'\n'"Available document IDs:"$'\n'"$(echo "$actual_ids" | sed 's/^/  - /')"
    fi
}

validate_sidebar_ids() {
    log "Validating sidebar document references..."

    local current_docs_root="${WEBSITE_DIR}/docs"

    validate_sidebar_file "${SIDEBARS_FILE}" "${current_docs_root}" "Current (Next)"

    local sidebar_file version_dir docs_root version_name
    while IFS= read -r -d '' sidebar_file; do
        version_dir="$(basename "${sidebar_file}" -sidebars.json)"
        version_name="${version_dir#version-}"
        docs_root="${VERSIONED_DOCS_DIR}/${version_dir}"
        validate_sidebar_file "${sidebar_file}" "${docs_root}" "Version ${version_name}"
    done < <(find "${VERSIONED_SIDEBARS_DIR}" -type f -name "version-*-sidebars.json" -print0)

    log "Sidebar document references validated."
}

validate_frontmatter() {
    log "Validating frontmatter..."

    local file relative_path issues=0
    local current_docs_root="${WEBSITE_DIR}/docs"

    # Validate current (Next) docs
    while IFS= read -r -d '' file; do
        relative_path="${file#${WEBSITE_DIR}/}"

        if ! awk 'BEGIN{fm=0} /^---$/{fm=(fm?0:1);next} fm && /^title:/ {t=1} END {exit (t ? 0 : 1)}' "$file" 2>/dev/null; then
            warn "${relative_path}: missing 'title' in frontmatter"
            ((issues++)) || true
        fi
    done < <(find "${current_docs_root}" -type f \( -name "*.md" -o -name "*.mdx" \) -print0)

    # Validate versioned docs
    while IFS= read -r -d '' file; do
        relative_path="${file#${WEBSITE_DIR}/}"

        if ! awk 'BEGIN{fm=0} /^---$/{fm=(fm?0:1);next} fm && /^title:/ {t=1} END {exit (t ? 0 : 1)}' "$file" 2>/dev/null; then
            warn "${relative_path}: missing 'title' in frontmatter"
            ((issues++)) || true
        fi
    done < <(find "${VERSIONED_DOCS_DIR}" -type f \( -name "*.md" -o -name "*.mdx" \) -print0)

    if [[ $issues -gt 0 ]]; then
        fail "Frontmatter validation failed with $issues issue(s)"
    fi

    log "Frontmatter validated."
}

validate_internal_links() {
    log "Internal links are validated by Docusaurus during npm run build."
}

install_dependencies() {
    log "Installing dependencies in ${WEBSITE_DIR}/..."

    if [[ -f "${WEBSITE_DIR}/package-lock.json" ]]; then
        npm ci --prefix "${WEBSITE_DIR}"
    else
        npm install --prefix "${WEBSITE_DIR}"
    fi
}

build_documentation() {
    log "Building documentation..."
    npm run build --prefix "${WEBSITE_DIR}"
}

main() {
    log "Checking documentation build..."

    check_dependencies
    validate_sidebar_ids
    validate_frontmatter
    validate_internal_links
    install_dependencies
    build_documentation

    log "Documentation build completed successfully."
}

main "$@"
