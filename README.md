JMX Exporter
=====

JMX to Prometheus exporter: a collector that can configurably scrape and
expose mBeans of a JMX target.

This exporter is intended to be run as a Java Agent, exposing a HTTP server
and serving metrics of the local JVM. It can be also run as a standalone
HTTP server and scrape remote JMX targets, but this has various
disadvantages, such as being harder to configure and being unable to expose
process metrics (e.g., memory and CPU usage). Running the exporter as a Java
Agent is thus strongly encouraged.

## Running the Java Agent

The Java agent is available in two versions with identical functionality:
* [jmx_prometheus_javaagent-0.17.0.jar](https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.17.0/jmx_prometheus_javaagent-0.17.0.jar) requires Java >= 7.
* [jmx_prometheus_javaagent-0.17.0_java6.jar](https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent_java6/0.17.0/jmx_prometheus_javaagent_java6-0.17.0.jar) is compatible with Java 6.

Both versions are built from the same code and differ only in the versions of the bundled dependencies.

To run as a Java agent, download one of the JARs and run:
```
java -javaagent:./jmx_prometheus_javaagent-0.17.0.jar=12345:config.yaml -jar yourJar.jar
```

Metrics will now be accessible at [http://localhost:12345/metrics](http://localhost:12345/metrics).
To bind the java agent to a specific IP change the port number to `host:port`.

A minimal `config.yaml` looks like this:

```yaml
rules:
- pattern: ".*"
```

Example configurations can be found in the `example_configs/` directory.

## Running the Standalone HTTP Server

The HTTP server is available in two versions with identical functionality:
* [jmx_prometheus_httpserver-0.17.0.jar](https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_httpserver/0.17.0/jmx_prometheus_httpserver-0.17.0.jar) requires Java >= 7.
* [jmx_prometheus_httpserver-0.17.0_java6.jar](https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_httpserver_java6/0.17.0/jmx_prometheus_httpserver_java6-0.17.0.jar) is compatible with Java 6.

Both versions are built from the same code and differ only in the versions of the bundled dependencies.

To run the standalone HTTP server, download one of the JARs and run:

```
java -jar jmx_prometheus_httpserver-0.17.0.jar 12345 config.yaml
```

Metrics will now be accessible at [http://localhost:12345/metrics](http://localhost:12345/metrics).
To bind the java agent to a specific IP change the port number to `host:port`.

The standalone HTTP server will read JMX remotely over the network. Therefore, you need to specify
either `hostPort` or `jmxUrl` in `config.yaml` to tell the HTTP server where the JMX beans can be accessed.

A minimal `config.yaml` looks like this:

```yaml
hostPort: localhost:9999
rules:
- pattern: ".*"
```

As stated above, it is recommended to run JMX exporter as a Java agent and not as a standalone HTTP server.

## Building

`./mvnw package` to build.

## Configuration
The configuration is in YAML. An example with all possible options:
```yaml
---
startDelaySeconds: 0
hostPort: 127.0.0.1:1234
username: 
password: 
jmxUrl: service:jmx:rmi:///jndi/rmi://127.0.0.1:1234/jmxrmi
ssl: false
lowercaseOutputName: false
lowercaseOutputLabelNames: false
whitelistObjectNames: ["org.apache.cassandra.metrics:*"]
blacklistObjectNames: ["org.apache.cassandra.metrics:type=ColumnFamily,*"]
rules:
  - pattern: 'org.apache.cassandra.metrics<type=(\w+), name=(\w+)><>Value: (\d+)'
    name: cassandra_$1_$2
    value: $3
    valueFactor: 0.001
    labels: {}
    help: "Cassandra metric $1 $2"
    cache: false
    type: GAUGE
    attrNameSnakeCase: false
```
Name     | Description
---------|------------
startDelaySeconds | start delay before serving requests. Any requests within the delay period will result in an empty metrics set.
hostPort   | The host and port to connect to via remote JMX. If neither this nor jmxUrl is specified, will talk to the local JVM.
username   | The username to be used in remote JMX password authentication.
password   | The password to be used in remote JMX password authentication.
jmxUrl     | A full JMX URL to connect to. Should not be specified if hostPort is.
ssl        | Whether JMX connection should be done over SSL. To configure certificates you have to set following system properties:<br/>`-Djavax.net.ssl.keyStore=/home/user/.keystore`<br/>`-Djavax.net.ssl.keyStorePassword=changeit`<br/>`-Djavax.net.ssl.trustStore=/home/user/.truststore`<br/>`-Djavax.net.ssl.trustStorePassword=changeit`
lowercaseOutputName | Lowercase the output metric name. Applies to default format and `name`. Defaults to false.
lowercaseOutputLabelNames | Lowercase the output metric label names. Applies to default format and `labels`. Defaults to false.
whitelistObjectNames | A list of [ObjectNames](http://docs.oracle.com/javase/6/docs/api/javax/management/ObjectName.html) to query. Defaults to all mBeans.
blacklistObjectNames | A list of [ObjectNames](http://docs.oracle.com/javase/6/docs/api/javax/management/ObjectName.html) to not query. Takes precedence over `whitelistObjectNames`. Defaults to none.
rules      | A list of rules to apply in order, processing stops at the first matching rule. Attributes that aren't matched aren't collected. If not specified, defaults to collecting everything in the default format.
pattern           | Regex pattern to match against each bean attribute. The pattern is not anchored. Capture groups can be used in other options. Defaults to matching everything.
attrNameSnakeCase | Converts the attribute name to snake case. This is seen in the names matched by the pattern and the default format. For example, anAttrName to an\_attr\_name. Defaults to false.
name              | The metric name to set. Capture groups from the `pattern` can be used. If not specified, the default format will be used. If it evaluates to empty, processing of this attribute stops with no output.
value             | Value for the metric. Static values and capture groups from the `pattern` can be used. If not specified the scraped mBean value will be used.
valueFactor       | Optional number that `value` (or the scraped mBean value if `value` is not specified) is multiplied by, mainly used to convert mBean values from milliseconds to seconds.
labels            | A map of label name to label value pairs. Capture groups from `pattern` can be used in each. `name` must be set to use this. Empty names and values are ignored. If not specified and the default format is not being used, no labels are set.
help              | Help text for the metric. Capture groups from `pattern` can be used. `name` must be set to use this. Defaults to the mBean attribute description, domain, and name of the attribute.
cache             | Whether to cache bean name expressions to rule computation (match and mismatch). Not recommended for rules matching on bean value, as only the value from the first scrape will be cached and re-used. This can increase performance when collecting a lot of mbeans. Defaults to `false`.
type              | The type of the metric, can be `GAUGE`, `COUNTER` or `UNTYPED`. `name` must be set to use this. Defaults to `UNTYPED`.

Metric names and label names are sanitized. All characters other than `[a-zA-Z0-9:_]` are replaced with underscores,
and adjacent underscores are collapsed. There's no limitations on label values or the help text.

A minimal config is `{}`, which will connect to the local JVM and collect everything in the default format.
Note that the scraper always processes all mBeans, even if they're not exported.

Example configurations for javaagents can be found at  https://github.com/prometheus/jmx_exporter/tree/master/example_configs

### Pattern input
The format of the input matches against the pattern is
```
domain<beanpropertyName1=beanPropertyValue1, beanpropertyName2=beanPropertyValue2, ...><key1, key2, ...>attrName: value
```

Part     | Description
---------|------------
domain   | Bean name. This is the part before the colon in the JMX object name.
beanPropertyName/Value | Bean properties. These are the key/values after the colon in the JMX object name.
keyN     | If composite or tabular data is encountered, the name of the attribute is added to this list.
attrName | The name of the attribute. For tabular data, this will be the name of the column. If `attrNameSnakeCase` is set, this will be converted to snake case.
value    | The value of the attribute.

No escaping or other changes are made to these values, with the exception of if `attrNameSnakeCase` is set.
The default help includes this string, except for the value.

### Default format
The default format will transform beans in a way that should produce sane metrics in most cases. It is
```
domain_beanPropertyValue1_key1_key2_...keyN_attrName{beanpropertyName2="beanPropertyValue2", ...}: value
```
If a given part isn't set, it'll be excluded.

## Testing

The JMX exporter uses [Testcontainers](https://www.testcontainers.org/) to run tests with different Java versions.
You need to have Docker installed to run these tests.

You can run the tests with:

```
./mvnw verify
```

## Debugging

You can start the jmx's scraper in standalone mode in order to debug what is called 

```
git clone https://github.com/prometheus/jmx_exporter.git
cd jmx_exporter
./mvnw package
java -cp collector/target/collector*.jar  io.prometheus.jmx.JmxScraper  service:jmx:rmi:your_url
```

To get finer logs (including the duration of each jmx call),
create a file called logging.properties with this content:

```
handlers=java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level=ALL
io.prometheus.jmx.level=ALL
io.prometheus.jmx.shaded.io.prometheus.jmx.level=ALL
```

Add the following flag to your Java invocation:

`-Djava.util.logging.config.file=/path/to/logging.properties`


## Installing

A Debian binary package is created as part of the build process and it can
be used to install an executable into `/usr/bin/jmx_exporter` with configuration
in `/etc/jmx_exporter/jmx_exporter.yaml`.
