#!/usr/bin/env bash
# Script to run a java application for testing jmx4prometheus.
env

RUNDIR=${RUNDIR:-/usr/local/jmx_exporter}
echo RUNDIR $RUNDIR
BIND_HOST=${BIND_HOST:-localhost}
echo BINDING TO HOST:PORT $BIND_HOST:39390
CONFIG_FILE_DIR=${CONFIG_FILE_DIR:-/usr/local/jmx_exporter/jmx_prometheus_httpserver/config/config.yml}
echo GOING TO USE $CONFIG_FILE_DIR AS INPUT CONFIG 
VERSION=$JMX_PROMETHEUS_VERSION
echo using jar jmx_prometheus_httpserver-${VERSION}-jar-with-dependencies.jar
java -jar  $RUNDIR/jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-${VERSION}-jar-with-dependencies.jar $BIND_HOST $CONFIG_FILE_DIR

