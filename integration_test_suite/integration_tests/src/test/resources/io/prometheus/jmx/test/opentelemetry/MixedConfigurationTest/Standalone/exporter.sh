#!/bin/bash

export OTEL_EXPORTER_OTLP_ENDPOINT="http://prometheus:9090/api/v1/otlp"

java \
  -Xmx512M \
  -jar jmx_prometheus_standalone.jar exporter.yaml
