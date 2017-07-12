#!/bin/bash

set -x

mvn_evaluate="org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate  -Dexpression=project.version"
mvn $mvn_evaluate > /dev/null
VERSION=$(mvn $mvn_evaluate | grep -v -e "^\[INFO\]") 

docker build -t amimimor/jmx-prometheus:$VERSION -f docker/Dockerfile .

