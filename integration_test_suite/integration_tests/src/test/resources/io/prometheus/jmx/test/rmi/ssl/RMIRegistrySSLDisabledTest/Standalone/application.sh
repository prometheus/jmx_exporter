#!/bin/bash

#
# Code to run the test with RedHat UBI images
#
# When running on RedHat UBI images, testcontainers maps
# the files as the current user, but the application runs
# as "jboss" on UBI8 images and "default" on UBI9 images
# preventing the chmod commands to change permissions on
# the jmxremote.access and jmxremote.password files.
#
# The code copies the files to /tmp as the current user
# then performs a chmod to change permissions.
#

JMXREMOTE_ACCESS=jmxremote.access
JMXREMOTE_PASSWORD=jmxremote.password

WHOAMI=$(whoami)
if [ "${WHOAMI}" = "jboss" ] || [ "${WHOAMI}" = "default" ];
then
  cp ${JMXREMOTE_ACCESS} /tmp/jmxremote.access
  cp ${JMXREMOTE_PASSWORD} /tmp/jmxremote.password
  chmod go-rwx /tmp/jmxremote.access
  chmod go-rwx /tmp/jmxremote.password
  JMXREMOTE_ACCESS=/tmp/jmxremote.access
  JMXREMOTE_PASSWORD=/tmp/jmxremote.password
else
  chmod go-rwx jmxremote.access
  chmod go-rwx jmxremote.password
fi

export RMI_REGISTRY_SSL_DISABLED=true

java \
  -Xmx512M \
  -Dcom.sun.management.jmxremote=true \
  -Dcom.sun.management.jmxremote.authenticate=true \
  -Dcom.sun.management.jmxremote.password.file=${JMXREMOTE_PASSWORD} \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.access.file=${JMXREMOTE_ACCESS} \
  -Dcom.sun.management.jmxremote.ssl=true \
  -Dcom.sun.management.jmxremote.rmi.port=8888 \
  -Djavax.net.ssl.keyStore=localhost.pkcs12 \
  -Djavax.net.ssl.keyStorePassword=changeit \
  -Djavax.net.ssl.keyStoreType=pkcs12 \
  -Djavax.net.ssl.trustStore=localhost.pkcs12 \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -Djavax.net.ssl.trustStoreType=pkcs12 \
  -jar jmx_example_application.jar