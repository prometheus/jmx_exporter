jmx4prometheus
=====

JMX to prometheus bridge.

A webserver that exposes mBeans of a JMX target in JSON format suitable
for http://github.com/prometheus.

# About
A prometheus server makes a JSON request to a the jmx4prometheus server which then
scrapes all beans of a target JMX port on another process. The target can
either be statically configured in the configuration or passed in by
prometheus server as a query string parameter.
Jmx4prometheus can return all mBeans (that are numeric) or be configured with
whitelist/blacklist behaviour.

## Building and Running

`mvn package` to build.
See `run_jmx4prometheus_server.sh` for a sample script that runs the webserver.

## Testing

`./tests/integration_test.py` to test. This is a simple integration test that
starts up a sample bean exposing process and runs jmx4prometheus to scrape it
(Doesn't include the prometheus server integration).

## Installing

A Debian binary package is created as part of the build process and it can 
be used to install an executable into `/usr/local/bin/jmx4prometheus` with configuration
in `/etc/jmx4prometheus/jmx4prometheus_config.json`.

## TODO

* Need to replace the JSON exposing code with prometheus' own client side code.
* Add configurable logging  

