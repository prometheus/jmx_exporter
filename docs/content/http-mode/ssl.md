---
title: SSL
weight: 3
---

HTTP mode supports configuring SSL (HTTPS) access using either a JKS or PKCS12 format keystore.

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

#  Complex YAML Configuration Examples

Integration tests  provide complex/concrete examples of application and YAML configuration files.

- [integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test](https://github.com/prometheus/jmx_exporter/tree/main/integration_test_suite/integration_tests/src/test/resources/io/prometheus/jmx/test)
