#!/bin/bash

java \
  -Xmx512M \
  -Djavax.net.ssl.keyStore=localhost.pkcs12 \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -Djavax.net.ssl.keyStoreType=pkcs12 \
  -Djavax.net.ssl.trustStore=localhost.pkcs12 \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -Djavax.net.ssl.trustStoreType=pkcs12 \
  -jar jmx_prometheus_httpserver.jar 8888 exporter.yaml