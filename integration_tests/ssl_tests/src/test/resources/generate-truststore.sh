#!/bin/bash

# This script was used to generate the "truststore" file.

if [[ ! -f keystore ]] ; then
    echo "Run ./generate-keystore.sh first." 2>&1
    exit 1
fi

# export the certificate
keytool -export -alias alias1 -keystore keystore -file keystore.cer -storepass password
# import the certificate to the truststore
keytool -import -alias alias1 -file keystore.cer -keystore truststore -storepass password