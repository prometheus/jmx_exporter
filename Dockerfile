#TO_BUILD mvn clean package && mvn docker:build --pl jmx_prometheus_httpserver
#TO_RUN docker run -p 8280:80 -v <pathToConfig>/config.json:/opt/prometheus/conf/config.json jmx_prometheus_httpserver:<version>
FROM java:openjdk-8u45-jre

ADD jmx_prometheus_httpserver/target/*-jar-with-dependencies.jar /opt/prometheus/jms_prometheus_httpserver.jar

VOLUME /opt/prometheus/conf
WORKDIR /opt/prometheus

EXPOSE 80

CMD java -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=5555 -jar jms_prometheus_httpserver.jar 80 /opt/prometheus/conf/config.json