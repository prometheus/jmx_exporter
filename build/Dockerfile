FROM maven:3.8.5-openjdk as javabuilder

RUN mkdir /opt/jmx_exporter
COPY . /opt/jmx_exporter
WORKDIR /opt/jmx_exporter/
RUN mvn package -DskipTests

FROM anapsix/alpine-java:8

RUN apk update && apk upgrade && apk --update add curl && rm -rf /tmp/* /var/cache/apk/*

ENV VERSION 0.17.1
ENV JAR jmx_prometheus_httpserver-$VERSION-jar-with-dependencies.jar
COPY --from=javabuilder /opt/jmx_exporter/jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-$VERSION-SNAPSHOT.jar /opt/jmx_exporter/$JAR

FROM quay.io/sysdig/sysdig-mini-ubi:1.3.1 as ubi
RUN mkdir -p /opt/jmx_exporter
RUN microdnf --assumeyes --disableplugin=subscription-manager --nodocs update && \
    microdnf --assumeyes --disableplugin=subscription-manager --nodocs install curl java-1.8.0-openjdk && \
    microdnf --assumeyes --disableplugin=subscription-manager --nodocs clean all && \
    rm -rf /var/cache/yum
ENV VERSION 0.17.1
ENV JAR jmx_prometheus_httpserver-$VERSION.jar
COPY --from=javabuilder /opt/jmx_exporter/jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-$VERSION-SNAPSHOT.jar /opt/jmx_exporter/$JAR
COPY --chown=sysdig:sysdig build/start.sh /opt/jmx_exporter
COPY --chown=sysdig:sysdig build/config.yml /opt/jmx_exporter
RUN chmod +x /opt/jmx_exporter/start.sh
USER sysdig:sysdig

CMD ["/opt/jmx_exporter/start.sh"]