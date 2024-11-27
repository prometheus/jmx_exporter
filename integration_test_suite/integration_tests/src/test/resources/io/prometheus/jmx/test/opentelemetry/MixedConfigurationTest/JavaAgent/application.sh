#!/bin/bash

export OTEL_EXPORTER_OTLP_ENDPOINT="http://prometheus:9090/api/v1/otlp"

java \
  -Xmx512M \
  -javaagent:jmx_prometheus_javaagent.jar=exporter.yaml \
  -jar jmx_example_application.jar
