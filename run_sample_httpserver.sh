#!/usr/bin/env bash
# Script to run a java application for testing jmx4prometheus.

version=$(sed -n -e 's#.*<version>\(.*-SNAPSHOT\)</version>#\1#p' pom.xml)

# Note: You can use localhost:5556 instead of 5556 for configuring socket hostname.
java -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=5555 -jar jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-${version}-jar-with-dependencies.jar 5556 example_configs/httpserver_sample_config.yml
