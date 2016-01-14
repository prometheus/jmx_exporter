#!/usr/bin/env bash

set -e

TAG=$1

if [ -z "$TAG" ] ; then
  echo "Tag not specified - eg 0.1.0"
  echo "Note: you need to have the dist zip present"
  echo
  echo "Usage:"
  echo "./docker-build.sh <tag>"
  echo
  echo "Example:"
  echo "./docker-build.sh latest"
  exit
fi

docker build -t "docker.movio.co/jmx-exporter:$TAG" .
