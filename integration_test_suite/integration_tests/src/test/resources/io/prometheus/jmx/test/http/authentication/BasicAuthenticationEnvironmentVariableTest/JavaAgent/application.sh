#!/bin/bash

# Export the environment variables used for authentication
export USERNAME=Prometheus
export SECRET=secret

java \
  -Xmx512M \
  -javaagent:jmx_prometheus_javaagent.jar=8888:exporter.yaml \
  -jar jmx_example_application.jar
