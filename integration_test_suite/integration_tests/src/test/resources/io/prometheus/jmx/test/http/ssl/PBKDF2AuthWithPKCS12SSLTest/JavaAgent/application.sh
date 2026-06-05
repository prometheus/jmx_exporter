#!/bin/bash

java \
  -Xmx512M \
  -Djavax.net.ssl.keyStore=keystore.pkcs12 \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -Djavax.net.ssl.keyStoreType=pkcs12 \
  -javaagent:jmx_prometheus_javaagent.jar=8888:exporter.yaml \
  -jar jmx_example_application.jar
