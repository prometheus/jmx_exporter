#!/bin/bash

# Create a per-execution temp directory (safe for parallel test runs)
TMPDIR=$(mktemp -d)

cp jmxremote.access ${TMPDIR}/jmxremote.access
cp jmxremote.password ${TMPDIR}/jmxremote.password
chmod go-rwx ${TMPDIR}/jmxremote.access
chmod go-rwx ${TMPDIR}/jmxremote.password

java \
  -Xmx512M \
  -Dcom.sun.management.jmxremote=true \
  -Dcom.sun.management.jmxremote.authenticate=true \
  -Dcom.sun.management.jmxremote.password.file=${TMPDIR}/jmxremote.password \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.registry.ssl=false \
  -Dcom.sun.management.jmxremote.access.file=${TMPDIR}/jmxremote.access \
  -Dcom.sun.management.jmxremote.rmi.port=9999 \
  -Dcom.sun.management.jmxremote.ssl=false \
  -jar /common/jmx_example_application.jar
