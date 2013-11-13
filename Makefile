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
