#!/bin/bash

chmod go-rwx jmxremote.access
chmod go-rwx jmxremote.password

java \
  -Dcom.sun.management.jmxremote=true \
  -Dcom.sun.management.jmxremote.authenticate=true \
  -Dcom.sun.management.jmxremote.access.file=jmxremote.access \
  -Dcom.sun.management.jmxremote.local.only=false \
  -Dcom.sun.management.jmxremote.password.file=jmxremote.password \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.registry.ssl=false \
  -Dcom.sun.management.jmxremote.rmi.port=9999 \
  -Dcom.sun.management.jmxremote.ssl.need.client.auth=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Djavax.net.ssl.keyStore=keystore.jks \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -Djavax.net.ssl.trustStore=truststore.jks \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -jar jmx_example_application.jar