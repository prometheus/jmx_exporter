#!/bin/bash

java \
  -Xmx512M \
  -Djavax.net.ssl.keyStore=keystore.jks \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -javaagent:/common/jmx_prometheus_javaagent.jar=8888:exporter.yaml \
  -jar /common/jmx_example_application.jar
