#!/bin/bash

java \
  -Xmx512M \
  -javaagent:jmx_prometheus_javaagent.jar=exporter.yaml \
  -jar jmx_example_application.jar
