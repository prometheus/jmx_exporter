#!/bin/bash

java \
  -Xmx512M \
  -Djavax.net.ssl.keyStore=localhost.jks \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -jar jmx_prometheus_standalone.jar 8888 exporter.yaml
