#!/bin/bash

export USERNAME=Prometheus
export PASSWORD=secret

java \
  -Xmx512M \
  -jar /common/jmx_prometheus_standalone.jar 8888 exporter.yaml
