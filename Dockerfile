FROM maven:3.6.3-adoptopenjdk-11

ADD ./ /build

WORKDIR /build
RUN mvn clean package


FROM openjdk:11-slim
COPY --from=0 /build/jmx_prometheus_javaagent/target/jmx_prometheus_javaagent-*.jar /usr/share/jmx_exporter/
COPY --from=0 /build/jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-*-jar-with-dependencies.jar /usr/share/jmx_exporter/ 

CMD ["/bin/bash"]
