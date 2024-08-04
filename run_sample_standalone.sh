#!/usr/bin/env bash

# Script to run a java application for testing the Prometheus JMX Standalone exporter.

VERSION=$(grep "<version>" pom.xml | head -1 | sed -n 's/.*<version>\(.*\)<\/version>.*/\1/p')

# Note: You can use localhost:5556 instead of 5556 for configuring socket hostname.

java \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.port=5555 \
  -jar jmx_prometheus_standalone/target/jmx_prometheus_standalone-"${VERSION}".jar 5556 \
  example_configs/standalone_sample_config.yml
