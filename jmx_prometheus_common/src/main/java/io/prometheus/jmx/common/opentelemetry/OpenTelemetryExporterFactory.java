package io.prometheus.jmx.common.opentelemetry;

import static java.lang.String.format;

import io.prometheus.jmx.common.configuration.ConvertToInteger;
import io.prometheus.jmx.common.configuration.ConvertToMap;
import io.prometheus.jmx.common.configuration.ConvertToMapAccessor;
import io.prometheus.jmx.common.configuration.ConvertToString;
import io.prometheus.jmx.common.configuration.ValidateIntegerInRange;
import io.prometheus.jmx.common.configuration.ValidateIsURL;
import io.prometheus.jmx.common.configuration.ValidateStringIsNotBlank;
import io.prometheus.jmx.common.http.ConfigurationException;
import io.prometheus.jmx.common.yaml.YamlMapAccessor;
import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** Class to implement OpenTelemetryExporterFactory */
public class OpenTelemetryExporterFactory {

    private static final int NO_INTERVAL = -1;

    /** Constructor */
    private OpenTelemetryExporterFactory() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to create an OpenTelemetryExporter using the supplied arguments
     *
     * @param prometheusRegistry prometheusRegistry
     * @param exporterYamlFile exporterYamlFile
     * @return OpenTelemetryExporter OpenTelemetryExporter
     * @throws ConfigurationException ConfigurationException
     */
    public OpenTelemetryExporter createOpenTelemetryExporter(
            PrometheusRegistry prometheusRegistry, File exporterYamlFile)
            throws ConfigurationException {
        if (exporterYamlFile == null) {
            throw new IllegalArgumentException("exporterYamlFile is null");
        }

        try {
            try (Reader reader = new FileReader(exporterYamlFile)) {
                Map<Object, Object> yamlMap = new Yaml().load(reader);
                YamlMapAccessor rootYamlMapAccessor = new YamlMapAccessor(yamlMap);

                YamlMapAccessor openTelemetryYamlMapAccessor =
                        rootYamlMapAccessor
                                .get("/openTelemetry")
                                .map(
                                        new ConvertToMapAccessor(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                                + " /openTelemetry must be a map")))
                                .orElse(new YamlMapAccessor());

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
                                .orElse(null);

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
                                .orElse(null);

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
                                                                + " an integer greater than 0")))
                                .orElse(NO_INTERVAL);

                Map<String, String> headers =
                        openTelemetryYamlMapAccessor
                                .get("/headers")
                                .map(
                                        new ConvertToMap(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                                + " /openTelemetry/headers"
                                                                + " must be a map")))
                                .orElse(null);

                OpenTelemetryExporter.Builder openTelemetryExporterBuilder =
                        OpenTelemetryExporter.builder();

                openTelemetryExporterBuilder.registry(prometheusRegistry);

                if (endpoint != null) {
                    openTelemetryExporterBuilder.endpoint(endpoint);
                }

                if (protocol != null) {
                    openTelemetryExporterBuilder.protocol(protocol);
                }

                if (interval != NO_INTERVAL) {
                    openTelemetryExporterBuilder.intervalSeconds(interval);
                }

                if (headers != null) {
                    headers.forEach(
                            (name, value) -> {
                                if (name != null
                                        && !name.trim().isEmpty()
                                        && value != null
                                        && !value.trim().isEmpty()) {
                                    openTelemetryExporterBuilder.header(name.trim(), value.trim());
                                }
                            });
                }

                return openTelemetryExporterBuilder.buildAndStart();
            }
        } catch (IOException e) {
            throw new ConfigurationException(
                    format("Exception loading file [%s]", exporterYamlFile), e);
        }
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
