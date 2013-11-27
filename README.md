jmmix
=====

JMX to prometheus bridge.

A webserver that exposes all mBeans of a JMX target in JSON format suitable
for http://github.com/prometheus.

## Building and Running

`mvn package` to build.
See `run_jmmix_server.sh` for a sample script that runs the webserver.

## Testing

`./tests/integration_test.py` to test.

## Installing

A Debian binary package is created as part of the build process and it can 
be used to install an executable into `/usr/local/bin/jmmix` with configuration
in `/etc/jmmix/jmmix_config.json`.

