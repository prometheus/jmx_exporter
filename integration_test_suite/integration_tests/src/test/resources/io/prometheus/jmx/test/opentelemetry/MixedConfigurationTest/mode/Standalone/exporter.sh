#!/bin/bash

export OTEL_EXPORTER_OTLP_ENDPOINT="http://prometheus:9090/api/v1/otlp"

java \
  -Xmx512M \
  -jar /common/jmx_prometheus_standalone.jar 8888 exporter.yaml
