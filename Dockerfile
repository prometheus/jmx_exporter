FROM java:openjdk-8u45-jre

ADD jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-0.3-SNAPSHOT-jar-with-dependencies.jar /opt/prometheus/jms_prometheus_httpserver.jar

ENV CONFIG_FILE sample_config.json

VOLUME /opt/prometheus/conf
WORKDIR /opt/prometheus

EXPOSE 80

CMD java -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=5555 -jar jms_prometheus_httpserver.jar 80 /opt/prometheus/conf/$CONFIG_FILE