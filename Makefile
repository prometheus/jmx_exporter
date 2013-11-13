#
# Yep, a makefile.
# Feel free to insert Ant,Maven,XMLz here...
#

BASEDIR=$(realpath $(dir $(firstword $(MAKEFILE_LIST))))
BUILDDIR=build
# TODO: change cwd?


SOURCES=$(shell find src/ -name \*.java)
OBJECTS=$(patsubst src/%.java,build/%.class,$(SOURCES))
TEST_SOURCES=$(shell find tests/ -name \*.java)
TEST_OBJECTS=$(patsubst %.java,build/%.class,$(TEST_SOURCES))

all: compile test

JARS=$(shell ls ${BASEDIR}/jars/*.jar | tr '\n' ':')

compile: $(BUILDDIR) $(OBJECTS)

compile_tests: compile $(TEST_OBJECTS)

test: compile $(TEST_OBJECTS)
	./tests/integration_test.py

$(BUILDDIR):
	mkdir -p $(BUILDDIR)
	mkdir -p $(BUILDDIR)/tests

build/tests/com/typingduck/jmmix/Main.class: tests/com/typingduck/jmmix/Main.java build/tests/com/typingduck/jmmix/CassandraMetricsMBean.class
	javac -cp ${JARS}:$(BUILDDIR):$(BUILDDIR)/tests -d $(BUILDDIR)/tests $<

build/tests/%.class: tests/%.java
	javac -cp ${JARS}:$(BUILDDIR):$(BUILDDIR)/tests -d $(BUILDDIR)/tests $<

build/com/typingduck/jmmix/WebServer.class: src/com/typingduck/jmmix/WebServer.java build/com/typingduck/jmmix/JmxScraper.class build/com/typingduck/jmmix/PrometheusBeanFormatter.class
	javac -cp ${JARS}:$(BUILDDIR) -d $(BUILDDIR) $<

build/%.class: src/%.java
	javac -cp ${JARS}:$(BUILDDIR) -d $(BUILDDIR) $<



#####
# Copyright (c) 2013 typingduck
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# The Software shall be used for Good, not Evil.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#####
