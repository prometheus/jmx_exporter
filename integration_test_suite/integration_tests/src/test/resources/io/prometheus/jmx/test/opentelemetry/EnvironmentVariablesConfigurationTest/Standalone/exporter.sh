#!/bin/bash

export OTEL_EXPORTER_OTLP_ENDPOINT="http://prometheus:9090/api/v1/otlp"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_METRIC_EXPORT_INTERVAL="1"

java \
  -Xmx512M \
  -jar jmx_prometheus_standalone.jar exporter.yaml
