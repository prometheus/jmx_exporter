#!/bin/sh

if [ -z "$SERVICE_PORT" ]; then
  SERVICE_PORT=5556
fi

if [ -z "$JVM_OPTS" ]; then
  JVM_OPTS="-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=5555"
fi

if [ -z "$CONFIG_YML" ]; then
  CONFIG_YML=/opt/jmx_exporter/config.yml
fi

java $JVM_OPTS -jar /opt/jmx_exporter/jmx_prometheus_httpserver-$VERSION-jar-with-dependencies.jar $SERVICE_PORT $CONFIG_YML
