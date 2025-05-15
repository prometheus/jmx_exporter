/*
 * Copyright (C) 2024-present The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.common.opentelemetry;

import static java.lang.String.format;

import io.prometheus.jmx.common.configuration.ConvertToInteger;
import io.prometheus.jmx.common.configuration.ConvertToMap;
import io.prometheus.jmx.common.configuration.ConvertToMapAccessor;
import io.prometheus.jmx.common.configuration.ConvertToString;
import io.prometheus.jmx.common.configuration.ValidateIntegerInRange;
import io.prometheus.jmx.common.configuration.ValidateIsURL;
import io.prometheus.jmx.common.configuration.ValidateMapValues;
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

                OpenTelemetryExporter.Builder openTelemetryExporterBuilder =
                        OpenTelemetryExporter.builder();

                openTelemetryExporterBuilder.registry(prometheusRegistry);

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
                        .ifPresent(openTelemetryExporterBuilder::endpoint);

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
                        .ifPresent(openTelemetryExporterBuilder::protocol);

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
                        .ifPresent(openTelemetryExporterBuilder::intervalSeconds);

                openTelemetryYamlMapAccessor
                        .get("/timeoutSeconds")
                        .map(
                                new ConvertToInteger(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/timeoutSeconds"
                                                        + " must be an integer")))
                        .map(
                                new ValidateIntegerInRange(
                                        1,
                                        Integer.MAX_VALUE,
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/timeoutSeconds must be"
                                                        + " an integer greater than 0")))
                        .ifPresent(openTelemetryExporterBuilder::timeoutSeconds);

                openTelemetryYamlMapAccessor
                        .get("/headers")
                        .map(
                                new ConvertToMap(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for /openTelemetry/headers"
                                                        + " must be a map")))
                        .map(
                                new ValidateMapValues(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for /openTelemetry/headers"
                                                    + " must contains valid string keys/values")))
                        .ifPresent(
                                headers -> headers.forEach(openTelemetryExporterBuilder::header));

                openTelemetryYamlMapAccessor
                        .get("/resourceAttributes")
                        .map(
                                new ConvertToMap(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /openTelemetry/resourceAttributes must be a"
                                                    + " map")))
                        .map(
                                new ValidateMapValues(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/resourceAttributes must"
                                                        + " contains valid string keys/values")))
                        .ifPresent(
                                headers ->
                                        headers.forEach(
                                                openTelemetryExporterBuilder::resourceAttribute));

                openTelemetryYamlMapAccessor
                        .get("/serviceInstanceId")
                        .map(
                                new ConvertToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/serviceInstanceId"
                                                        + " must be a string")))
                        .map(
                                new ValidateStringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/serviceInstanceId"
                                                        + " must not be blank")))
                        .ifPresent(openTelemetryExporterBuilder::serviceInstanceId);

                openTelemetryYamlMapAccessor
                        .get("/serviceNamespace")
                        .map(
                                new ConvertToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/serviceNamespace"
                                                        + " must be a string")))
                        .map(
                                new ValidateStringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/serviceNamespace"
                                                        + " must not be blank")))
                        .ifPresent(openTelemetryExporterBuilder::serviceNamespace);

                openTelemetryYamlMapAccessor
                        .get("/serviceName")
                        .map(
                                new ConvertToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/serviceName"
                                                        + " must be a string")))
                        .map(
                                new ValidateStringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/serviceName"
                                                        + " must not be blank")))
                        .ifPresent(openTelemetryExporterBuilder::serviceName);

                openTelemetryYamlMapAccessor
                        .get("/serviceVersion")
                        .map(
                                new ConvertToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/serviceVersion"
                                                        + " must be a string")))
                        .map(
                                new ValidateStringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /openTelemetry/serviceVersion"
                                                        + " must not be blank")))
                        .ifPresent(openTelemetryExporterBuilder::serviceVersion);

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
