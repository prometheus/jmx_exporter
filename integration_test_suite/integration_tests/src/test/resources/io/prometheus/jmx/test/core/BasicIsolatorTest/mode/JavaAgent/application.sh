#!/bin/bash

java \
  -Xmx512M \
  -javaagent:/common/jmx_prometheus_isolator_javaagent.jar=/common/jmx_prometheus_javaagent.jar=8888:exporter.yaml,/common/jmx_prometheus_javaagent.jar=8889:exporter2.yaml,/common/jmx_prometheus_javaagent.jar=8890:exporter3.yaml \
  -jar /common/jmx_example_application.jar
