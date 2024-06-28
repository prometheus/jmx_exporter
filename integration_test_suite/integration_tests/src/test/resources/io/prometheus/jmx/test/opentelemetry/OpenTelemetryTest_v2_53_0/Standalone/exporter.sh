#!/bin/bash

java \
  -Xmx512M \
  -jar jmx_prometheus_httpserver.jar 8888 exporter.yaml