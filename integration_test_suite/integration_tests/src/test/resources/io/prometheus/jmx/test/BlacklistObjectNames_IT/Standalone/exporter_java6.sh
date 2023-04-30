#!/bin/bash

java \
  -Xmx512M \
  -jar jmx_prometheus_httpserver_java6.jar 8888 exporter.yaml