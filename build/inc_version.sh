#!/bin/bash

bump_patch() {
  bump_version $1
}

bump_version() {
  version="$1"
  major=0
  minor=0
  patch=0
  regular_expression="([0-9]+).([0-9]+).([0-9]+)"
  if [[ $version =~ $regular_expression ]]; then
    major="${BASH_REMATCH[1]}"
    minor="${BASH_REMATCH[2]}"
    patch="${BASH_REMATCH[3]}"
  fi
  if [[ "$2" == "m" ]]; then
    minor=$(( $minor + 1 ))
  elif [[ "$2" == "p" ]]; then
    patch=$(( $patch + 1 ))
  elif [[ "$2" == "M" ]]; then
    major=$(( $major + 1 ))
  else
    patch=$(( $patch + 1 ))
  fi
  echo "${major}.${minor}.${patch}"
}


bump_patch ${CURRENT_VERSION}
