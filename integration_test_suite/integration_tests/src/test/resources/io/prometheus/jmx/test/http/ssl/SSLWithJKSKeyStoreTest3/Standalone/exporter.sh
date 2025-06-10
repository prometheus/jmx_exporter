#!/bin/bash

export SECRET=changeit

java \
  -Xmx512M \
  -jar jmx_prometheus_standalone.jar 8888 exporter.yaml
