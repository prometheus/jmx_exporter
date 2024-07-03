package io.prometheus.jmx.common.opentelemetry;

import static java.lang.String.format;

import io.prometheus.jmx.common.configuration.ConvertToInteger;
import io.prometheus.jmx.common.configuration.ConvertToMapAccessor;
import io.prometheus.jmx.common.configuration.ConvertToString;
import io.prometheus.jmx.common.configuration.ValidateIntegerInRange;
import io.prometheus.jmx.common.configuration.ValidateIsURL;
import io.prometheus.jmx.common.configuration.ValidateStringIsNotBlank;
import io.prometheus.jmx.common.http.ConfigurationException;
import io.prometheus.jmx.common.yaml.YamlMapAccessor;
import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** Class to implement OpenTelemetryExporterFactory */
public class OpenTelemetryExporterFactory {

    /** Constructor */
    private OpenTelemetryExporterFactory() {
        // DO NOTHING
    }

    /**
     * Method to create an OpenTelemetryExporter using the supplied arguments
     *
     * @param exporterYamlFile exporterYamlFile
     * @return OpenTelemetryExporter OpenTelemetryExporter
     * @throws ConfigurationException ConfigurationException
     */
    public OpenTelemetryExporter create(File exporterYamlFile) throws ConfigurationException {
        if (exporterYamlFile == null) {
            throw new IllegalArgumentException("exporterYamlFile is null");
        }

        OpenTelemetryExporter openTelemetryExporter = null;

        try {
            try (Reader reader = new FileReader(exporterYamlFile)) {
                Map<Object, Object> yamlMap = new Yaml().load(reader);
                YamlMapAccessor rootYamlMapAccessor = new YamlMapAccessor(yamlMap);

                if (rootYamlMapAccessor.containsPath("/openTelemetry")) {
                    YamlMapAccessor openTelemetryYamlMapAccessor =
                            rootYamlMapAccessor
                                    .get("/openTelemetry")
                                    .map(
                                            new ConvertToMapAccessor(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                    + " /openTelemetry")))
                                    .orElseThrow(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for /openTelemetry"));

                    String endpoint =
                            openTelemetryYamlMapAccessor
                                    .get("/endpoint")
                                    .map(
                                            new ConvertToString(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                    + " /openTelemetry/endpoint"
                                                                    + " must be a string")))
                                    .map(
                                            new ValidateStringIsNotBlank(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                    + " /openTelemetry/endpoint"
                                                                    + " must not be blank")))
                                    .map(
                                            new ValidateIsURL(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                    + " /openTelemetry/endpoint"
                                                                    + " must be a URL")))
                                    .orElse("http://localhost:4317");

                    String protocol =
                            openTelemetryYamlMapAccessor
                                    .get("/protocol")
                                    .map(
                                            new ConvertToString(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                    + " /openTelemetry/protocol"
                                                                    + " must be a string")))
                                    .map(
                                            new ValidateStringIsNotBlank(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                    + " /openTelemetry/protocol"
                                                                    + " must not be blank")))
                                    .orElse("grpc");

                    int interval =
                            openTelemetryYamlMapAccessor
                                    .get("/interval")
                                    .map(
                                            new ConvertToInteger(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                    + " /openTelemetry/interval"
                                                                    + " must be an integer")))
                                    .map(
                                            new ValidateIntegerInRange(
                                                    1,
                                                    Integer.MAX_VALUE,
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                + " /openTelemetry/interval must be"
                                                                + " between greater than 0")))
                                    .orElse(60);

                    openTelemetryExporter =
                            OpenTelemetryExporter.builder()
                                    .endpoint(endpoint)
                                    .protocol(protocol)
                                    .intervalSeconds(interval)
                                    .buildAndStart();
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException(
                    format("Exception loading file [%s]", exporterYamlFile), e);
        }

        return openTelemetryExporter;
    }

    /**
     * Method to get an instance of the OpenTelemetryExporterFactory
     *
     * @return the OpenTelemetryExporterFactory
     */
    public static OpenTelemetryExporterFactory getInstance() {
        return SingletonHolder.SINGLETON;
    }

    /** Class to hold the singleton */
    private static class SingletonHolder {

        /** The singleton */
        public static final OpenTelemetryExporterFactory SINGLETON =
                new OpenTelemetryExporterFactory();
    }
}
