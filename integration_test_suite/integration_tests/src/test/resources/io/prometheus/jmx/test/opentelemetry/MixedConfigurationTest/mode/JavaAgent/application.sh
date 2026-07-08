#!/bin/bash

export OTEL_EXPORTER_OTLP_ENDPOINT="http://prometheus:9090/api/v1/otlp"

java \
  -Xmx512M \
  -javaagent:/common/jmx_prometheus_javaagent.jar=8888:exporter.yaml \
  -jar /common/jmx_example_application.jar
