#!/usr/bin/env bash
# Sample script to run jmmix webserver against a local target.
JMMIX_PORT=5556
JMX_PORT=5555

dctry=$(dirname $0)
builddir=${dctry}/build/

export CLASSPATH="${builddir}:${dctry}/jars/jetty-6.1.24.jar:${dctry}/jars/jetty-util-6.1.24.jar:${dctry}/jars/servlet-api-2.5.jar"
java com.typingduck.jmmix.WebServer -- 5556 localhost:5555

