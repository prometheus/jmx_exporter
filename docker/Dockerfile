FROM openjdk:8u131-jdk-alpine

MAINTAINER "Amit Mor" <amimimor@gmail.com>

RUN apk update && apk add bash maven
ARG RUNDIR
ENV RUNDIR ${RUNDIR:-/usr/local/jmx_exporter}
WORKDIR $RUNDIR
ADD jmx_prometheus_httpserver/ jmx_prometheus_httpserver/
ADD jmx_prometheus_javaagent/ jmx_prometheus_javaagent/
ADD collector/ collector/
ADD pom.xml pom.xml
# get the version from the pom.xml
ENV mvn_evaluate "org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate  -Dexpression=project.version"
# let maven import the plugin dependcies
RUN mvn $mvn_evaluate > /dev/null
# extract the project version and pass it into the container as env var, to be consumed by the start.sh script
RUN VERSION=$(mvn $mvn_evaluate | grep -v -e "^\[INFO\]") && echo export JMX_PROMETHEUS_VERSION=$VERSION > /root/.bashrc && printf "\nFound version: %s" $VERSION
# compile
RUN mvn compile package
ADD docker/start.sh /start.sh
RUN chmod +x /start.sh
# config volume
RUN mkdir -p $RUNDIR/jmx_prometheus_httpserver/config/ 
VOLUME $RUNDIR/jmx_prometheus_httpserver/config/
# ports
EXPOSE 39390
# cmd
CMD /start.sh
