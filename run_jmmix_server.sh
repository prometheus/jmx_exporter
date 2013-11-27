#!/usr/bin/env bash
# Sample script to run jmmix webserver against a local target.
# Uses configuration is 'sample_config.json'.

dctry=$(dirname $0)
builddir=${dctry}/target/

java -jar ${builddir}/jmmix-0.1-jar-with-dependencies.jar -- -c "sample_config.json"
