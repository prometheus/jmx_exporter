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
# Build and run the JMH benchmarks for the jmx_prometheus_common module.
#
# The benchmarks are packaged as a self-contained executable jar by the
# jmx_prometheus_common-benchmarks module. This script builds that jar and then
# runs JMH against it.
#
# Usage: ./scripts/jmx_prometheus_common-benchmarks.sh [options] [-- <jmh args>]
#
# Options:
#   --no-build   Skip building jmx_prometheus_common-benchmarks/target/benchmarks.jar
#   --quick      Run with reduced JMH settings (-f 1 -wi 3 -i 3)
#   -h, --help   Show this help
#
# Environment:
#   JAVA_HOME    Java installation to build/run with
#   JAVA_OPTS    JVM options passed to the JMH JVM (e.g. "-Xmx2g")
#   JMH_ARGS     Additional JMH arguments (space separated)
#
# Examples:
#   ./scripts/jmx_prometheus_common-benchmarks.sh --quick
#   ./scripts/jmx_prometheus_common-benchmarks.sh --quick '.*PlaintextAuthenticationBenchmark.*'
#   ./scripts/jmx_prometheus_common-benchmarks.sh --no-build -- -f 1 -wi 3 -i 3 ".*Credentials.*"
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR
readonly ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly MODULE_DIR="${ROOT_DIR}/jmx_prometheus_common-benchmarks"
readonly BENCHMARKS_JAR="${MODULE_DIR}/target/benchmarks.jar"

log_info() {
    echo "> $*"
}

log_error() {
    echo "! $*" >&2
}

usage() {
    cat <<'EOF'
Usage: ./scripts/jmx_prometheus_common-benchmarks.sh [options] [-- <jmh args>]

Options:
  --no-build   Skip building jmx_prometheus_common-benchmarks/target/benchmarks.jar
  --quick      Run with reduced JMH settings (-f 1 -wi 3 -i 3)
  -h, --help   Show this help

Environment:
  JAVA_HOME    Java installation to build/run with
  JAVA_OPTS    JVM options passed to the JMH JVM (e.g. "-Xmx2g")
  JMH_ARGS     Additional JMH arguments (space separated)

Examples:
  ./scripts/jmx_prometheus_common-benchmarks.sh --quick
  ./scripts/jmx_prometheus_common-benchmarks.sh --quick '.*PlaintextAuthenticationBenchmark.*'
  ./scripts/jmx_prometheus_common-benchmarks.sh --no-build -- -f 1 -wi 3 -i 3 ".*Credentials.*"
EOF
}

build() {
    log_info "Building jmx_prometheus_common-benchmarks (tests skipped)"
    (
        cd "${ROOT_DIR}"
        ./mvnw -B -pl jmx_prometheus_common-benchmarks -am -DskipTests package
    )
}

resolve_java() {
    if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
        printf '%s' "${JAVA_HOME}/bin/java"
    else
        printf '%s' "java"
    fi
}

main() {
    local build_benchmarks=true
    local quick=false
    local -a jmh_args=()

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --no-build)
                build_benchmarks=false
                shift
                ;;
            --quick)
                quick=true
                shift
                ;;
            --)
                shift
                jmh_args+=("$@")
                break
                ;;
            -h | --help)
                usage
                return 0
                ;;
            *)
                jmh_args+=("$1")
                shift
                ;;
        esac
    done

    if [[ "${build_benchmarks}" == true ]]; then
        build
    fi

    if [[ ! -f "${BENCHMARKS_JAR}" ]]; then
        log_error "Benchmark jar not found: ${BENCHMARKS_JAR}"
        log_error "Run without --no-build to build it."
        exit 1
    fi

    local java_cmd
    java_cmd="$(resolve_java)"

    local -a default_args=()
    if [[ "${quick}" == true ]]; then
        default_args=(-f 1 -wi 3 -i 3)
    fi

    if [[ -n "${JMH_ARGS:-}" ]]; then
        # Intentionally word-split JMH_ARGS so multiple arguments can be supplied.
        # shellcheck disable=SC2206
        local -a environmental_jmh_args=(${JMH_ARGS})
        jmh_args+=("${environmental_jmh_args[@]}")
    fi

    log_info "Running: ${java_cmd} ${JAVA_OPTS:-} -jar ${BENCHMARKS_JAR} ${default_args[*]} ${jmh_args[*]}"

    # Intentionally word-split JAVA_OPTS so multiple JVM options can be supplied.
    # shellcheck disable=SC2086
    exec "${java_cmd}" ${JAVA_OPTS:-} -jar "${BENCHMARKS_JAR}" "${default_args[@]}" "${jmh_args[@]}"
}

main "$@"
