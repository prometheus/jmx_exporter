#!/bin/bash

# Export the secret to a file for integration testing
echo -n "secret" > /tmp/secret.txt

java \
  -Xmx512M \
  -jar jmx_prometheus_standalone.jar 8888 exporter.yaml
