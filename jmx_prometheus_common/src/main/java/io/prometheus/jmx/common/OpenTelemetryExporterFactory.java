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

/**
 * Factory for creating and configuring OpenTelemetry exporters for the JMX exporter.
 *
 * <p>This factory creates OpenTelemetry exporters with support for:
 *
 * <ul>
 *   <li>Configurable endpoint URL
 *   <li>Configurable protocol (grpc, http/protobuf, http/json)
 *   <li>Configurable export interval and timeout
 *   <li>Custom headers for authentication/authorization
 *   <li>Service metadata (name, namespace, version, instance ID)
 *   <li>Resource attributes for telemetry correlation
 * </ul>
 *
 * <p>This class is not instantiable and all methods are static.
 *
 * <p>Thread-safety: This class is thread-safe.
 */
public class OpenTelemetryExporterFactory {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private OpenTelemetryExporterFactory() {
        // INTENTIONALLY BLANK
    }

    /**
     * Creates and starts an OpenTelemetry exporter with the specified configuration.
     *
     * <p>The OpenTelemetry exporter is configured based on the YAML configuration file under
     * the {@code openTelemetry} path. If the path is not present, this method returns
     * {@code null}.
     *
     * @param prometheusRegistry the Prometheus registry for metric collection, must not be
     *     {@code null}
     * @param exporterYamlFile the YAML configuration file, must not be {@code null}
     * @return the started OpenTelemetry exporter instance, or {@code null} if OpenTelemetry is
     *     not configured
     * @throws ConfigurationException if the configuration is invalid
     * @throws IllegalArgumentException if {@code exporterYamlFile} is {@code null}
     */
    public static OpenTelemetryExporter createAndStartOpenTelemetryExporter(
            PrometheusRegistry prometheusRegistry, File exporterYamlFile) throws ConfigurationException {
        if (exporterYamlFile == null) {
            throw new IllegalArgumentException("exporterYamlFile is null");
        }

        try {
            MapAccessor rootMapAccessor = MapAccessor.of(YamlSupport.loadYaml(exporterYamlFile));

            if (rootMapAccessor.containsPath("/openTelemetry")) {
                MapAccessor openTelemetryMapAccessor = rootMapAccessor
                        .get("/openTelemetry")
                        .map(new ToMapAccessor(ConfigurationException.supplier(
                                "Invalid configuration for" + " /openTelemetry must be a map")))
                        .orElse(null);

                OpenTelemetryExporter.Builder openTelemetryExporterBuilder = OpenTelemetryExporter.builder();

                if (openTelemetryMapAccessor != null) {
                    openTelemetryExporterBuilder.registry(prometheusRegistry);

                    openTelemetryMapAccessor
                            .get("/endpoint")
                            .map(new ToString(ConfigurationException.supplier(
                                    "Invalid configuration for" + " /openTelemetry/endpoint" + " must be a string")))
                            .map(new StringIsNotBlank(ConfigurationException.supplier(
                                    "Invalid configuration for" + " /openTelemetry/endpoint" + " must not be blank")))
                            .map(new IsURL(ConfigurationException.supplier(
                                    "Invalid configuration for" + " /openTelemetry/endpoint" + " must be a URL")))
                            .ifPresent(openTelemetryExporterBuilder::endpoint);

                    openTelemetryMapAccessor
                            .get("/protocol")
                            .map(new ToString(ConfigurationException.supplier(
                                    "Invalid configuration for" + " /openTelemetry/protocol" + " must be a string")))
                            .map(new StringIsNotBlank(ConfigurationException.supplier(
                                    "Invalid configuration for" + " /openTelemetry/protocol" + " must not be blank")))
                            .ifPresent(openTelemetryExporterBuilder::protocol);

                    openTelemetryMapAccessor
                            .get("/interval")
                            .map(new ToInteger(ConfigurationException.supplier(
                                    "Invalid configuration for" + " /openTelemetry/interval" + " must be an integer")))
                            .map(new IntegerInRange(
                                    1,
                                    Integer.MAX_VALUE,
                                    ConfigurationException.supplier("Invalid configuration for"
                                            + " /openTelemetry/interval must be"
                                            + " an integer greater than 0")))
                            .ifPresent(openTelemetryExporterBuilder::intervalSeconds);

                    openTelemetryMapAccessor
                            .get("/timeoutSeconds")
                            .map(new ToInteger(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/timeoutSeconds"
                                    + " must be an integer")))
                            .map(new IntegerInRange(
                                    1,
                                    Integer.MAX_VALUE,
                                    ConfigurationException.supplier("Invalid configuration for"
                                            + " /openTelemetry/timeoutSeconds must"
                                            + " be an integer greater than 0")))
                            .ifPresent(openTelemetryExporterBuilder::timeoutSeconds);

                    openTelemetryMapAccessor
                            .get("/headers")
                            .map(new ToMap(ConfigurationException.supplier(
                                    "Invalid configuration for" + " /openTelemetry/headers must be a" + " map")))
                            .map(new ValidMap(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/headers must"
                                    + " contains valid string"
                                    + " keys/values")))
                            .ifPresent(headers -> headers.forEach(openTelemetryExporterBuilder::header));

                    openTelemetryMapAccessor
                            .get("/resourceAttributes")
                            .map(new ToMap(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/resourceAttributes"
                                    + " must be a map")))
                            .map(new ValidMap(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/resourceAttributes"
                                    + " must contains valid string"
                                    + " keys/values")))
                            .ifPresent(headers -> headers.forEach(openTelemetryExporterBuilder::resourceAttribute));

                    openTelemetryMapAccessor
                            .get("/serviceInstanceId")
                            .map(new ToString(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/serviceInstanceId"
                                    + " must be a string")))
                            .map(new StringIsNotBlank(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/serviceInstanceId"
                                    + " must not be blank")))
                            .ifPresent(openTelemetryExporterBuilder::serviceInstanceId);

                    openTelemetryMapAccessor
                            .get("/serviceNamespace")
                            .map(new ToString(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/serviceNamespace"
                                    + " must be a string")))
                            .map(new StringIsNotBlank(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/serviceNamespace"
                                    + " must not be blank")))
                            .ifPresent(openTelemetryExporterBuilder::serviceNamespace);

                    openTelemetryMapAccessor
                            .get("/serviceName")
                            .map(new ToString(ConfigurationException.supplier(
                                    "Invalid configuration for" + " /openTelemetry/serviceName" + " must be a string")))
                            .map(new StringIsNotBlank(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/serviceName"
                                    + " must not be blank")))
                            .ifPresent(openTelemetryExporterBuilder::serviceName);

                    openTelemetryMapAccessor
                            .get("/serviceVersion")
                            .map(new ToString(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/serviceVersion"
                                    + " must be a string")))
                            .map(new StringIsNotBlank(ConfigurationException.supplier("Invalid configuration for"
                                    + " /openTelemetry/serviceVersion"
                                    + " must not be blank")))
                            .ifPresent(openTelemetryExporterBuilder::serviceVersion);
                }

                return openTelemetryExporterBuilder.buildAndStart();
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new ConfigurationException(format("Exception loading file [%s]", exporterYamlFile), e);
        }
    }
}
