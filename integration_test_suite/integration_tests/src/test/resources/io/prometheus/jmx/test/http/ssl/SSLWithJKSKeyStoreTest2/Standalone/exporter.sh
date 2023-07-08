#!/bin/bash

java \
  -Xmx128M \
  -jar jmx_prometheus_httpserver.jar 8888 exporter.yaml