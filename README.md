jmmix
=====

JMX to prometheus bridge.

A webserver that exposes all mBeans of a JMX target in JSON format suitable
for http://github.com/prometheus.

## Building and Running

`make compile` to build, `make` to build and run tests.
See `run_jmmix_server.sh` for a sample script that runs the webserver.

## Testing

`make test` currently runs integration tests.

## Directories:

* **jars/** For convienience I've manually included the few dependent jars in the repo.
* **src/**
* **tests/** Just has a python integration test script and a sample target currently.
