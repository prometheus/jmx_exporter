<picture>
  <img src="https://circleci.com/gh/prometheus/jmx_exporter.svg?style=shield"/>
</picture>

JMX Exporter
=====

JMX to Prometheus exporter: a collector that can configurable scrape and
expose mBeans of a JMX target.

This exporter is intended to be run as a Java Agent, exposing a HTTP server
and serving metrics of the local JVM. It can be also run as a standalone
HTTP server and scrape remote JMX targets, but this has various
disadvantages, such as being harder to configure and being unable to expose
process metrics (e.g., memory and CPU usage). In particular all the
`jvm_*` metrics like `jvm_classes_loaded_total`, `jvm_threads_current`,
`jvm_threads_daemon` and `jvm_memory_bytes_used` won't be availabe if
using the standalone http server.

### **NOTES**

**- Metrics are no longer served on the root (`/`) path - use the `/metrics` path ** 

**- Some JVM metric names have changed to conform with the [OpenMetrics](https://openmetrics.io/) specification.**

**- Dashboards will need to be changed if referencing the changed JVM metrics.**

https://prometheus.github.io/client_java/migration/simpleclient/#jvm-metrics


## Running the Java Agent

**Running the exporter as a Java agent is strongly encouraged.**

- [jmx_prometheus_javaagent-1.0.1.jar](https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/1.0.1/jmx_prometheus_javaagent-1.0.1.jar)

To run as a Java agent, download one of the JARs and run:

```
java -javaagent:./jmx_prometheus_javaagent-1.0.1.jar=12345:config.yaml -jar yourJar.jar
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

- [jmx_prometheus_httpserver-1.0.1.jar](https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_httpserver/1.0.1/jmx_prometheus_httpserver-1.0.1.jar)

To run the standalone HTTP server, download one of the JARs and run:

```
java -jar jmx_prometheus_httpserver-1.0.1.jar 12345 config.yaml
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

**NOTES**

**- `jvm_*` metrics will not be available if running in standalone mode**

**- standard Java MBeans that provide JVM metrics will be available using the standard Java MBean ObjectNames**

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
includeObjectNames: ["org.apache.cassandra.metrics:*"]
excludeObjectNames: ["org.apache.cassandra.metrics:type=ColumnFamily,*"]
autoExcludeObjectNameAttributes: true
excludeObjectNameAttributes:
  "java.lang:type=OperatingSystem":
    - "ObjectName"
  "java.lang:type=Runtime":
    - "ClassPath"
    - "SystemProperties"
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
includeObjectNames | A list of [ObjectNames](http://docs.oracle.com/javase/6/docs/api/javax/management/ObjectName.html) to query. Defaults to all mBeans.
excludeObjectNames | A list of [ObjectNames](http://docs.oracle.com/javase/6/docs/api/javax/management/ObjectName.html) to not query. Takes precedence over `includeObjectNames`. Defaults to none.
autoExcludeObjectNameAttributes | Whether to auto exclude [ObjectName](http://docs.oracle.com/javase/6/docs/api/javax/management/ObjectName.html) attributes that can't be converted to standard metrics types. Defaults to `true`. 
excludeObjectNameAttributes | A Map of [ObjectNames](http://docs.oracle.com/javase/6/docs/api/javax/management/ObjectName.html) with a list of attribute names to exclude. ObjectNames must be in canonical form. Both ObjectNames and attribute names are matched as a Strings (no regex.) Optional.
rules      | A list of rules to apply in order, processing stops at the first matching rule. Attributes that aren't matched aren't collected. If not specified, defaults to collecting everything in the default format.
pattern           | Regex pattern to match against each bean attribute. The pattern is not anchored. Capture groups can be used in other options. Defaults to matching everything.
attrNameSnakeCase | Converts the attribute name to snake case. This is seen in the names matched by the pattern and the default format. For example, anAttrName to an\_attr\_name. Defaults to false.
name              | The metric name to set. Capture groups from the `pattern` can be used. If not specified, the default format will be used. If it evaluates to empty, processing of this attribute stops with no output. An Additional suffix may be added to this name (e.g `_total` for type `COUNTER`)
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

**NOTE**

Both `whitelistObjectNames` and `blacklistObjectNames` are still supported for backward compatibility, but should be considered deprecated.

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

HTTP BASIC authentication supports using the following configuration algorithms:

- plaintext - plaintext password
- SHA-1 - SHA-1(`<salt>:<password>`)
- SHA-256 - SHA-256(`<salt>:<password>`) 
- SHA-512 - SHA-512(`<salt>:<password>`)
- PBKDF2WithHmacSHA1
- PBKDF2WithHmacSHA256
- PBKDF2WithHmacSHA512

---

Plaintext example:

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      password: secret
```

---

SHA-256 example using a salted password SHA-256(`<salt>:<password>`) with a password of `secret`

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      passwordHash: 2bf7ed4906ac065bde39f7508d6102a6cdd7153a929ea883ff6cd04442772c99
      algorithm: SHA-256
      salt: U9i%=N+m]#i9yvUV:bA/3n4X9JdPXf=n
```

---

PBKDF2WithHmacSHA256 example with a password of `secret`

```yaml
httpServer:
  authentication:
    basic:
      username: Prometheus
      passwordHash: A1:0E:4E:62:F7:1E:0B:59:0A:32:EA:CC:7C:65:37:1F:6D:A6:F1:F1:ED:3F:73:ED:C9:65:19:37:21:5B:6D:4E:9D:C6:61:DF:B5:BF:BB:16:B8:9A:50:14:57:CE:3D:14:67:73:A3:71:1B:87:3B:C4:B1:0E:DC:2D:0B:10:65:D6:F5:B6:DA:07:DD:EE:DA:AC:9C:60:CD:B4:59:0C:C9:CB:A7:3D:7E:30:3E:43:83:E9:E4:13:34:A1:F1:87:5C:24:46:8E:13:90:A6:66:E1:A6:F3:0B:5A:E7:14:8A:98:6A:81:2B:B6:F8:EF:95:D4:82:7E:FB:5E:2D:D3:24:FE:96
      algorithm: PBKDF2WithHmacSHA256
      salt: U9i%=N+m]#i9yvUV:bA/3n4X9JdPXf=n
```

- iterations = `600000` (default value for PBKDF2WithHmacSHA256 )
- keyLength = `128` bits (default value)

**Notes**

- PBKDF2WithHmacSHA1 default iterations = `1300000`
- PBKDF2WithHmacSHA256 default iterations = `600000`
- PBKDF2WithHmacSHA512 default iterations = `210000`
- default keyLength = `128` (bits)

### Generation of `passwordHash`

- `sha1sum`, `sha256sum`, and `sha512sum` can be used to generate the `passwordHash`
- `openssl` can be used to generate a PBKDF2WithHmac based algorithm `passwordHash`

---

## HTTPS support (optional)

HTTPS support can be configured using either a JKS or PKCS12 format keystore via two possible methods:

- Exporter YAML configuration


- System properties

**Notes**

- Keystore type is dependent on the Java version


- Exporter YAML configuration overrides System properties

### Configuration (using Exporter YAML)

1. Add configuration to your exporter YAML file

```yaml
httpServer:
  ssl:
    keyStore:
      filename: localhost.jks
      password: changeit
    certificate:
      alias: localhost
```

2. Create a keystore and add your certificate

### Configuration (using System properties)

1. Add configuration to your exporter YAML file

```yaml
httpServer:
  ssl:
    certificate:
      alias: localhost
```

2. Add your certificate to the application's Java keystore

The exporter YAML file `alias` should match the certificate alias of the certificate you want to use for the HTTPS server.

3. Define the application system properties for the Java keystore

```shell
-Djavax.net.ssl.keyStore=<keystore file> -Djavax.net.ssl.keyStorePassword=<keystore password>
```

---

## HTTP Thread Pool Configuration (optional)

The exporter thread pool can be configured via the exporter YAML file.

By default, a maximum of 10 threads is used.

### Configuration

```yaml
httpServer:
  threads:
    minimum: 1
    maximum: 10
    keepAliveTime: 120 # seconds
```

- `minimum` - minimum number of threads
- `maximum` - maximum number of threads
- `keepAliveTime` - thread keep-alive time in seconds

**Notes**

- If the work queue is full, the request will be blocked until space is available in the work queue for the request.

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
