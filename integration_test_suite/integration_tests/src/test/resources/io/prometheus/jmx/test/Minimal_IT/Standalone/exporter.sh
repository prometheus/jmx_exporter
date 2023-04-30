#!/bin/bash

java \
  -jar jmx_prometheus_httpserver.jar 8888 exporter.yaml