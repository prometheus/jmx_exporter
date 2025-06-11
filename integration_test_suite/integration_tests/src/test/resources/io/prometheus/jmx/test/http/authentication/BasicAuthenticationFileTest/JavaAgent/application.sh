#!/bin/bash

# Export the secret to a file for integration testing
echo -n "secret" > /tmp/secret.txt

java \
  -Xmx512M \
  -javaagent:jmx_prometheus_javaagent.jar=8888:exporter.yaml \
  -jar jmx_example_application.jar
