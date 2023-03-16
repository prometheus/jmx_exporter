#!/bin/bash

java \
  -Djavax.net.ssl.keyStore=keystore.jks \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -Djavax.net.ssl.trustStore=truststore.jks \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -jar jmx_prometheus_httpserver_java6.jar 8888 exporter.yaml