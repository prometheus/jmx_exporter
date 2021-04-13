FROM maven:3.6.3-openjdk as javabuilder

RUN mkdir /opt/jmx_exporter
COPY pom.xml /opt/jmx_exporter
COPY collector /opt/jmx_exporter/collector
COPY jmx_prometheus_httpserver /opt/jmx_exporter/jmx_prometheus_httpserver
COPY jmx_prometheus_javaagent /opt/jmx_exporter/jmx_prometheus_javaagent
WORKDIR /opt/jmx_exporter/
RUN mvn package

FROM anapsix/alpine-java:8

RUN apk update && apk upgrade && apk --update add curl && rm -rf /tmp/* /var/cache/apk/*

ENV VERSION 0.15.1
ENV JAR jmx_prometheus_httpserver-$VERSION-jar-with-dependencies.jar
COPY --from=javabuilder /opt/jmx_exporter/jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-$VERSION-SNAPSHOT-jar-with-dependencies.jar /opt/jmx_exporter/$JAR

RUN mkdir -p /opt/jmx_exporter
COPY build/start.sh /opt/jmx_exporter/
COPY build/config.yml /opt/jmx_exporter/
RUN chmod -R +x /opt/jmx_exporter

EXPOSE 5556
USER 1001

CMD ["/opt/jmx_exporter/start.sh"]