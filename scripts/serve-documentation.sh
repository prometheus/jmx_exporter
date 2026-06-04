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

SKIP_BUILD="false"
PORT=""

usage() {
    cat <<'EOF'
Usage: ./scripts/serve-documentation.sh [OPTIONS]

Build and serve the Prometheus JMX Exporter documentation site locally.

Options:
  --skip-build       Skip the build step (use if docs were already built).
  --port <port>      Port to serve on. Default: 3000 (Docusaurus default).
  -h, --help         Show this help text.

Examples:
  ./scripts/serve-documentation.sh
  ./scripts/serve-documentation.sh --port 8080
  ./scripts/serve-documentation.sh --skip-build
EOF
}

log() {
    echo "[INFO] $*"
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
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --skip-build)
                SKIP_BUILD="true"
                ;;
            --port)
                [[ -n "${2:-}" && "${2:-}" != --* ]] || fail "--port requires a value"
                PORT="$2"
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                usage >&2
                fail "Unknown argument: $1"
                ;;
        esac
        shift
    done
}

build_documentation() {
    log "Building documentation..."
    "${SCRIPT_DIR}/build-documentation.sh"
}

serve_documentation() {
    log "Serving documentation..."
    if [[ -n "${PORT}" ]]; then
        npm run serve --prefix "${WEBSITE_DIR}" -- --port "${PORT}"
    else
        npm run serve --prefix "${WEBSITE_DIR}"
    fi
}

main() {
    parse_args "$@"
    check_dependencies

    if [[ "${SKIP_BUILD}" == "false" ]]; then
        build_documentation
    else
        log "Skipping build (--skip-build)"
    fi

    serve_documentation
}

main "$@"
