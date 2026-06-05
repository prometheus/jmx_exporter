#!/bin/bash

export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_EXPORTER_OTLP_METRICS_INTERVAL="1"

java \
  -Xmx512M \
  -jar jmx_prometheus_standalone.jar exporter.yaml
