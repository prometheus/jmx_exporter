/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.common;

import static java.lang.String.format;

import io.prometheus.jmx.common.util.MapAccessor;
import io.prometheus.jmx.common.util.YamlSupport;
import io.prometheus.jmx.common.util.functions.IntegerInRange;
import io.prometheus.jmx.common.util.functions.IsURL;
import io.prometheus.jmx.common.util.functions.StringIsNotBlank;
import io.prometheus.jmx.common.util.functions.ToInteger;
import io.prometheus.jmx.common.util.functions.ToMap;
import io.prometheus.jmx.common.util.functions.ToMapAccessor;
import io.prometheus.jmx.common.util.functions.ToString;
import io.prometheus.jmx.common.util.functions.ValidMap;
import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;

/** Class to create an OpenTelemetryExporter */
public class OpenTelemetryExporterFactory {

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
    public static OpenTelemetryExporter createAndStartOpenTelemetryExporter(
            PrometheusRegistry prometheusRegistry, File exporterYamlFile)
            throws ConfigurationException {
        if (exporterYamlFile == null) {
            throw new IllegalArgumentException("exporterYamlFile is null");
        }

        try {
            MapAccessor rootMapAccessor = MapAccessor.of(YamlSupport.loadYaml(exporterYamlFile));

            if (rootMapAccessor.containsPath("/openTelemetry")) {
                MapAccessor openTelemetryMapAccessor =
                        rootMapAccessor
                                .get("/openTelemetry")
                                .map(
                                        new ToMapAccessor(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                                + " /openTelemetry must be a map")))
                                .orElse(null);

                OpenTelemetryExporter.Builder openTelemetryExporterBuilder =
                        OpenTelemetryExporter.builder();

                if (openTelemetryMapAccessor != null) {
                    openTelemetryExporterBuilder.registry(prometheusRegistry);

                    openTelemetryMapAccessor
                            .get("/endpoint")
                            .map(
                                    new ToString(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/endpoint"
                                                            + " must be a string")))
                            .map(
                                    new StringIsNotBlank(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/endpoint"
                                                            + " must not be blank")))
                            .map(
                                    new IsURL(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/endpoint"
                                                            + " must be a URL")))
                            .ifPresent(openTelemetryExporterBuilder::endpoint);

                    openTelemetryMapAccessor
                            .get("/protocol")
                            .map(
                                    new ToString(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/protocol"
                                                            + " must be a string")))
                            .map(
                                    new StringIsNotBlank(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/protocol"
                                                            + " must not be blank")))
                            .ifPresent(openTelemetryExporterBuilder::protocol);

                    openTelemetryMapAccessor
                            .get("/interval")
                            .map(
                                    new ToInteger(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/interval"
                                                            + " must be an integer")))
                            .map(
                                    new IntegerInRange(
                                            1,
                                            Integer.MAX_VALUE,
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/interval must be"
                                                            + " an integer greater than 0")))
                            .ifPresent(openTelemetryExporterBuilder::intervalSeconds);

                    openTelemetryMapAccessor
                            .get("/timeoutSeconds")
                            .map(
                                    new ToInteger(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/timeoutSeconds"
                                                            + " must be an integer")))
                            .map(
                                    new IntegerInRange(
                                            1,
                                            Integer.MAX_VALUE,
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/timeoutSeconds must"
                                                            + " be an integer greater than 0")))
                            .ifPresent(openTelemetryExporterBuilder::timeoutSeconds);

                    openTelemetryMapAccessor
                            .get("/headers")
                            .map(
                                    new ToMap(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/headers must be a"
                                                            + " map")))
                            .map(
                                    new ValidMap(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/headers must"
                                                            + " contains valid string"
                                                            + " keys/values")))
                            .ifPresent(
                                    headers ->
                                            headers.forEach(openTelemetryExporterBuilder::header));

                    openTelemetryMapAccessor
                            .get("/resourceAttributes")
                            .map(
                                    new ToMap(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/resourceAttributes"
                                                            + " must be a map")))
                            .map(
                                    new ValidMap(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/resourceAttributes"
                                                            + " must contains valid string"
                                                            + " keys/values")))
                            .ifPresent(
                                    headers ->
                                            headers.forEach(
                                                    openTelemetryExporterBuilder
                                                            ::resourceAttribute));

                    openTelemetryMapAccessor
                            .get("/serviceInstanceId")
                            .map(
                                    new ToString(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/serviceInstanceId"
                                                            + " must be a string")))
                            .map(
                                    new StringIsNotBlank(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/serviceInstanceId"
                                                            + " must not be blank")))
                            .ifPresent(openTelemetryExporterBuilder::serviceInstanceId);

                    openTelemetryMapAccessor
                            .get("/serviceNamespace")
                            .map(
                                    new ToString(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/serviceNamespace"
                                                            + " must be a string")))
                            .map(
                                    new StringIsNotBlank(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/serviceNamespace"
                                                            + " must not be blank")))
                            .ifPresent(openTelemetryExporterBuilder::serviceNamespace);

                    openTelemetryMapAccessor
                            .get("/serviceName")
                            .map(
                                    new ToString(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/serviceName"
                                                            + " must be a string")))
                            .map(
                                    new StringIsNotBlank(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/serviceName"
                                                            + " must not be blank")))
                            .ifPresent(openTelemetryExporterBuilder::serviceName);

                    openTelemetryMapAccessor
                            .get("/serviceVersion")
                            .map(
                                    new ToString(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/serviceVersion"
                                                            + " must be a string")))
                            .map(
                                    new StringIsNotBlank(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /openTelemetry/serviceVersion"
                                                            + " must not be blank")))
                            .ifPresent(openTelemetryExporterBuilder::serviceVersion);
                }

                return openTelemetryExporterBuilder.buildAndStart();
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new ConfigurationException(
                    format("Exception loading file [%s]", exporterYamlFile), e);
        }
    }
}
