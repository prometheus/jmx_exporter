FROM docker.io/library/maven:3.6-jdk-11-slim

WORKDIR /build

COPY ./ /build

RUN mvn clean package

WORKDIR /build/jmx_prometheus_httpserver
RUN mvn clean package

FROM docker.io/library/openjdk:11-jre-slim

ENV JMX_REMOTE_PORT=5555 \
    JMX_EXPORTER_LISTEN_PORT=5556 \
    JMX_EXPORTER_CONFIG=/etc/jmx_exporter/config.yaml

COPY --from=0 /build/jmx_prometheus_javaagent/target/jmx_prometheus_javaagent-*-SNAPSHOT.jar /usr/share/jmx_exporter/jmx_prometheus_javaagent.jar
COPY --from=0 /build/jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-*-SNAPSHOT-jar-with-dependencies.jar /usr/share/jmx_exporter/jmx_prometheus_httpserver.jar
COPY example_configs/httpserver_sample_config.yml /etc/jmx_exporter/config.yaml
COPY entrypoint.sh /

EXPOSE 5556

ENTRYPOINT [ "/entrypoint.sh" ]
