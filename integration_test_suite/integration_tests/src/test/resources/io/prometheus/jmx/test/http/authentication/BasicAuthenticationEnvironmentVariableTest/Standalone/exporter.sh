#!/bin/bash

# Export the environment variable used for authentication
export SECRET=secret

java \
  -Xmx512M \
  -jar jmx_prometheus_standalone.jar 8888 exporter.yaml
