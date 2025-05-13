#!/bin/bash

java \
  -Xmx512M \
  -javaagent:jmx_prometheus_isolator_javaagent.jar=jmx_prometheus_javaagent.jar=8888:exporter.yaml,jmx_prometheus_javaagent.jar=8889:exporter2.yaml,jmx_prometheus_javaagent.jar=8890:exporter3.yaml \
  -jar jmx_example_application.jar
