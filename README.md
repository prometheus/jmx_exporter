JMX Exporter
=====

JMX to Prometheus exporter: a collector that can configurable scrape and
expose mBeans of a JMX target.

This exporter is intended to be run as a Java Agent, exposing a HTTP server
and serving metrics of the local JVM. It can be also run as a standalone
HTTP server and scrape remote JMX targets, but this has various
disadvantages, such as being harder to configure and being unable to expose
process metrics (e.g., memory and CPU usage). Running the exporter as a Java
Agent is thus strongly encouraged.

## Running the Java Agent

- [jmx_prometheus_javaagent-0.18.0.jar](https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.18.0/jmx_prometheus_javaagent-0.18.0.jar)

To run as a Java agent, download one of the JARs and run:

```
java -javaagent:./jmx_prometheus_javaagent-0.18.0.jar=12345:config.yaml -jar yourJar.jar
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

- [jmx_prometheus_httpserver-0.18.0.jar](https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_httpserver/0.18.0/jmx_prometheus_httpserver-0.18.0.jar)

To run the standalone HTTP server, download one of the JARs and run:

```
java -jar jmx_prometheus_httpserver-0.18.0.jar 12345 config.yaml
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

`./mvnw clean package` to build.

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

## HTTP Authentication (optional)

HTTP BASIC authentication it supported using various configuration formats.

---

Simple example (plaintext configuration value):

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      password: secret
```

---

More secure example (SHA-256 using a salted password `SHA256(<salt>:<password>)`):

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      hash: 2bf7ed4906ac065bde39f7508d6102a6cdd7153a929ea883ff6cd04442772c99
      algorithm: SHA-256
      salt: U9i%=N+m]#i9yvUV:bA/3n4X9JdPXf=n
```

**Notes**

- `algorithm` type must match a supported Java algorithm (JVM specific)

- `salt` is random string that you choose

---

Secure example (PBKDF2WithHmacSHA1):

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      password: B6:9C:5C:8A:10:3E:41:7B:BA:18:FC:E1:F2:0C:BC:D9:65:70:D3:53:AB:97:EE:2F:3F:A8:88:AF:43:EA:E6:D7:FB:70:14:23:F9:51:29:5C:3A:9F:65:C3:20:EE:09:C9:C6:8A:B7:D3:0A:E1:F3:10:2B:9B:36:3F:1F:B6:1D:52:A7:9C:CB:AD:55:25:46:C5:73:09:6C:38:9C:F2:FD:82:7F:90:E5:31:EF:7E:3E:6B:B2:0C:38:77:23:EC:3A:CF:29:F7:E5:4D:4E:CC:35:7A:C2:E5:CB:E3:B3:E5:09:2B:CC:B9:40:26:A4:28:E9:5F:2D:18:B2:14:41:E7:4D:5B
      algorithm: PBKDF2WithHmacSHA256
      salt: 98LeBWIjca
      iterations: 1000
      keyLength: 128
```

**Notes**

- `algorithm` type must match a supported Java secret key algorithm (JVM specific)

- `salt` is random string that you choose

- `iterations` is the number of iterations to use

- `keyLength` is the key length to use

- `hash` is the hash (as generated by `openssl`)

### Generating passwords

To generate a salted MessageDigest algorithm-based password, use the appropriate application for the algorithm

Example:

SHA-256 configuration hash generation

```
echo -n "98LeBWIjca:secret" | sha256sum
```

Example:

PBKDF2WithHmac256 configuration hash generation

Example:

```
openssl kdf -keylen 128 -kdfopt digest:SHA256 -kdfopt pass:secret -kdfopt salt:98LeBWIjca -kdfopt iter:1000 PBKDF2
```

---

## Integration Test Suite

The JMX exporter uses the [AntuBLUE Test Engine](https://github.com/antublue/test-engine) and [Testcontainers](https://www.testcontainers.org/) to run integration tests with different Java versions.

You need to have Docker installed to run the integration test suite.

Build and run the integration test suite:

```
./mvnw clean verify
```

**Notes**

- To run the integration tests in IntelliJ, you must build first the project from the parent (root) using Maven
  - The Maven build copies the core artifacts as resources to the `integration_tests` project 


- Additional information can be found in the [Integration Test Suite](https://github.com/prometheus/jmx_exporter/blob/main/integration_test_suite/README.md) readme.

## Debugging

You can start the JMX scraper in standalone mode in order to debug what is called 

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
