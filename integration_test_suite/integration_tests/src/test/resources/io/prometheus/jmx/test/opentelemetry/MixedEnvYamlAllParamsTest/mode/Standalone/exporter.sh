#!/bin/bash

export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_METRIC_EXPORT_INTERVAL="1"

java \
  -Xmx512M \
  -jar /common/jmx_prometheus_standalone.jar 8888 exporter.yaml
