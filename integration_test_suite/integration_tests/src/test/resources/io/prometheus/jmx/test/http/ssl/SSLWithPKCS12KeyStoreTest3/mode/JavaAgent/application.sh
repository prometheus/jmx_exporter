#!/bin/bash

export SECRET=changeit

java \
  -Xmx512M \
  -Djavax.net.ssl.keyStore=keystore.pkcs12 \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -Djavax.net.ssl.keyStoreType=pkcs12 \
  -javaagent:/common/jmx_prometheus_javaagent.jar=8888:exporter.yaml \
  -jar /common/jmx_example_application.jar
