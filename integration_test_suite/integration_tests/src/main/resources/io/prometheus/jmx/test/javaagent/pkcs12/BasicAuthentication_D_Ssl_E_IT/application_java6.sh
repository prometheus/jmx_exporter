#!/bin/bash

java \
  -Djavax.net.ssl.keyStore=keystore.pkcs12 \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -Djavax.net.ssl.trustStore=truststore.pkcs12 \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -javaagent:jmx_prometheus_javaagent_java6.jar=8888:exporter.yaml \
  -jar jmx_example_application.jar