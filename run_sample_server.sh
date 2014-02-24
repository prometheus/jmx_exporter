#!/usr/bin/env bash
# Script to run a java application for testing jmx4prometheus.
dctry=$(dirname $0)
builddir=${dctry}/target/

java -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=5555 -cp ${builddir}/jmx4prometheus-0.1-tests.jar com.typingduck.jmx4prometheus.Main

