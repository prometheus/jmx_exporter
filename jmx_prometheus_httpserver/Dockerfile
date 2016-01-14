FROM docker.movio.co/java-8:6.0.0

MAINTAINER Jack Hopner <jack@movio.co>

EXPOSE 9101 9180

# Move up once finialised
RUN mkdir /var/jmx-exporter \
  && groupadd -g 1012 atm \
  && useradd -u 1012 -g atm atm -d /var/jmx-exporter -s /bin/false \
  && chown -R atm:atm /var/jmx-exporter

WORKDIR /var/jmx-exporter
VOLUME /var/jmx-exporter/conf

COPY jmx_exporter_prometheus_httpserver.jar /var/jmx-exporter/bin/jmx_exporter_prometheus_httpserver.jar

COPY src/main/docker/bin /usr/local/bin/
COPY src/main/docker/etc /etc/

ENV JMX_REMOTE_PORT 9101
ENV JMX_EXPORTER_SERVER_PORT 9180
