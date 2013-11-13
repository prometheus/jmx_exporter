#!/usr/bin/env bash
# Sample script to run jmmix webserver against a local target.
# Uses configuration is 'sample_config.json'.

dctry=$(dirname $0)
builddir=${dctry}/build/

JARS=$(ls ${dctry}/jars/*.jar | tr '\n' ':')
export CLASSPATH="${builddir}:${JARS}"
java com.typingduck.jmmix.WebServer -- -c "sample_config.json"

