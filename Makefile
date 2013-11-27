#

defaul: test

build:
	mvn package

test: build
	./tests/integration_test.py

