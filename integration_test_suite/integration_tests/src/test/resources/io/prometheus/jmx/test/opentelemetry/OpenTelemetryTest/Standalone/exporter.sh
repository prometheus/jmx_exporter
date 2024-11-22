#!/bin/bash

java \
  -Xmx512M \
  -jar jmx_prometheus_standalone.jar exporter.yaml
