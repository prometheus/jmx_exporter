# Wildfly

Wildfly 8,9 and 10 use a custom MBeanServer implementation. To use JMX Exporter:

* As a _javaagent_ add:
`-Djavax.management.builder.initial=io.prometheus.jmx.ContainerNotReadyYet` to the javaagent line.
The class itself (ContainerNotReadyYet) does not exist and Wildfly overwrites the system property with a valid value during its startup. This *may* work with
other containers that use custom MBeanServers.

* As a remote scraper:
Add the `jboss-cli-client.jar` from the Wildfly installation to the classpath of JMX Exporter and use the Wildfly version specific JMX URL in the configuration file.
The JMX URL will be the same one as used to connect JConsole. The extra JAR is required because Wildfly uses a custom protocol to connect via the the HTTP(s) admin port.

