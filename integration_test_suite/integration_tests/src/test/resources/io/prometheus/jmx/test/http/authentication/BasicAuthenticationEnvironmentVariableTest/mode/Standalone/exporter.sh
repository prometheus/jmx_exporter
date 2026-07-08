#!/bin/bash

# Export the environment variables used for authentication
export USERNAME=Prometheus
export SECRET=secret

java \
  -Xmx512M \
  -jar /common/jmx_prometheus_standalone.jar 8888 exporter.yaml
