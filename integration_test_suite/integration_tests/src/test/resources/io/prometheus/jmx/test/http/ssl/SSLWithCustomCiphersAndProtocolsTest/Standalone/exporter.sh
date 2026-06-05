#!/bin/bash

java \
  -Xmx512M \
  -Djavax.net.ssl.keyStore=keystore.pkcs12 \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -Djavax.net.ssl.keyStoreType=pkcs12 \
  -jar jmx_prometheus_standalone.jar 8888 exporter.yaml
