#!/bin/bash

java \
  -Xmx512M \
  -javaagent:/common/jmx_prometheus_javaagent.jar=8888:exporter.yaml \
  -jar /common/jmx_example_application.jar