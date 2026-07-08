#!/bin/bash

export TEST_USERNAME=Prometheus
export TEST_PASSWORD=secret

java \
  -Xmx512M \
  -Djavax.net.ssl.keyStore=keystore.jks \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -jar /common/jmx_prometheus_standalone.jar 8888 exporter.yaml
