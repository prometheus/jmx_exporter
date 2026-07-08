#!/bin/bash

java \
  -Xmx512M \
  -jar /common/jmx_prometheus_standalone.jar 8888 exporter.yaml
