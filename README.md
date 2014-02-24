jmmix
=====

JMX to prometheus bridge.

A webserver that exposes mBeans of a JMX target in JSON format suitable
for http://github.com/prometheus.

# About
A prometheus server makes a JSON request to a the jmmix server which then
scrapes all beans of a target JMX port on another process. The target can
either be statically configured in the configuration or passed in by
prometheus server as a query string parameter.
Jmmix can return all mBeans (that are numeric) or be configured with
whitelist/blacklist behaviour.

## Building and Running

`mvn package` to build.
See `run_jmmix_server.sh` for a sample script that runs the webserver.

## Testing

`./tests/integration_test.py` to test.

## Installing

A Debian binary package is created as part of the build process and it can 
be used to install an executable into `/usr/local/bin/jmmix` with configuration
in `/etc/jmmix/jmmix_config.json`.

