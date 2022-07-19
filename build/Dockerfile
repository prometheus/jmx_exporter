FROM maven:3.8.6-openjdk-8 as javabuilder

ENV VERSION="0.17.1"
ENV JAR="jmx_prometheus_httpserver-${VERSION}.jar"

RUN mkdir /opt/jmx_exporter
COPY . /opt/jmx_exporter
WORKDIR /opt/jmx_exporter/
RUN mvn package -DskipTests

FROM quay.io/sysdig/sysdig-mini-ubi:1.3.2
RUN microdnf --assumeyes --disableplugin=subscription-manager --nodocs update && \
    microdnf --assumeyes --disableplugin=subscription-manager --nodocs install curl java-1.8.0-openjdk && \
    microdnf --assumeyes --disableplugin=subscription-manager --nodocs clean all && \
    rm -rf /var/cache/yum
RUN mkdir -p /opt/jmx_exporter
COPY --from=javabuilder --chown=sysdig:sysdig /opt/jmx_exporter/jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-${VERSION}-SNAPSHOT.jar /opt/jmx_exporter/${JAR}
COPY --chown=sysdig:sysdig build/start.sh /opt/jmx_exporter
COPY --chown=sysdig:sysdig build/config.yml /opt/jmx_exporter
RUN chmod 0755 /opt/jmx_exporter/start.sh
USER sysdig:sysdig

CMD ["/opt/jmx_exporter/start.sh"]
