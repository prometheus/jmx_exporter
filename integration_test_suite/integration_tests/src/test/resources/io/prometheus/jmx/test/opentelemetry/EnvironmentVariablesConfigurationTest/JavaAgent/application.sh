#!/bin/bash

export OTEL_EXPORTER_OTLP_ENDPOINT="http://prometheus:9090/api/v1/otlp"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_METRIC_EXPORT_INTERVAL="1"

java \
  -Xmx512M \
  -javaagent:jmx_prometheus_javaagent.jar=exporter.yaml \
  -jar jmx_example_application.jar
