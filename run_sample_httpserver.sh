#!/usr/bin/env bash
# Script to run a java application for testing jmx4prometheus.

java -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=5555 -jar jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-0.6-SNAPSHOT-jar-with-dependencies.jar 5556 sample_config.yml
