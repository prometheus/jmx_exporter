#!/bin/bash

# This script was used to generate the "keystore" file.

keytool -genkey -noprompt \
 -alias alias1 \
 -dname "CN=example.com" \
 -keyalg RSA \
 -keystore keystore \
 -storepass password \
 -keypass password

keytool -storepasswd -keystore keystore -storepass password -new password
