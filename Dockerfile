FROM java:8-jre
MAINTAINER  John-William Trenholm <jw@xomar.com>

WORKDIR /jmx_exporter
COPY jmx_prometheus_httpserver/target/jmx_prometheus_httpserver-0.*-SNAPSHOT-jar-with-dependencies.jar \
     /jmx_exporter/jmx_prometheus_httpserver.jar

EXPOSE 9098
ENTRYPOINT [ "java", "-Dcom.sun.management.jmxremote.ssl=false", \
	     "-Dcom.sun.management.jmxremote.authenticate=false", \
	     "-Dcom.sun.management.jmxremote.port=1099", "-jar", \
	     "/jmx_exporter/jmx_prometheus_httpserver.jar", \
	     "9098" ]

CMD ["/opt/jmx_exporter.yml" ]
