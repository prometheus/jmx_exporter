#!/bin/bash

java \
  -Xmx512M \
  -javaagent:jmx_prometheus_javaagent_java6.jar=8888:exporter.yaml \
  -jar jmx_example_application.jar