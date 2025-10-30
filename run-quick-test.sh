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
# Bash script to build, package, and run the tests (single Docker container)
#

(
  export JAVA_DOCKER_IMAGES="eclipse-temurin:25-alpine"
  export PROMETHEUS_DOCKER_IMAGES="prom/prometheus:v3.7.3"
  docker pull "$JAVA_DOCKER_IMAGES"
  docker pull "$PROMETHEUS_DOCKER_IMAGES"
  ./mvnw clean verify
) 2>&1 | tee quick-test.log
