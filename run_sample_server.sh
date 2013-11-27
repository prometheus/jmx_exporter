#!/usr/bin/env bash
# Script to run a java application for testing jmmix.
dctry=$(dirname $0)
builddir=${dctry}/target/

java -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=5555 -cp ${builddir}/jmmix-0.1-tests.jar com.typingduck.jmmix.Main

