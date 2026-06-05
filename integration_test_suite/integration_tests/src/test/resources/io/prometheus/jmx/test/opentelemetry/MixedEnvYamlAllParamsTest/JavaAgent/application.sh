#!/bin/bash

export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_EXPORTER_OTLP_METRICS_INTERVAL="1"

java \
  -Xmx512M \
  -javaagent:jmx_prometheus_javaagent.jar=exporter.yaml \
  -jar jmx_example_application.jar
