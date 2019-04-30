#!/usr/bin/env bash

java -Dcom.sun.management.jmxremote.ssl=false \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.port="${JMX_REMOTE_PORT}" \
     -jar /usr/share/jmx_exporter/jmx_prometheus_httpserver.jar "${JMX_EXPORTER_LISTEN_PORT}" "${JMX_EXPORTER_CONFIG}"
