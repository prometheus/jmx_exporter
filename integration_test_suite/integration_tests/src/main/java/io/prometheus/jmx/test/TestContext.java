/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/** Class to a TestState */
public class TestContext {

    private Network network;
    private GenericContainer<?> applicationContainer;
    private GenericContainer<?> exporterContainer;
    private String baseUrl;
    private HttpClient httpClient;

    /** Constructor */
    public TestContext() {
        // DO NOTHING
    }

    /**
     * Method to set the Network
     *
     * @param network network
     * @return this
     */
    public TestContext network(Network network) {
        this.network = network;
        return this;
    }

    /**
     * Method to get the Network
     *
     * @return the Network
     */
    public Network network() {
        return network;
    }

    /**
     * Method to set the application container
     *
     * @param applicationContainer application container
     * @return this
     */
    public TestContext applicationContainer(GenericContainer<?> applicationContainer) {
        this.applicationContainer = applicationContainer;
        return this;
    }

    /**
     * Method to get the application container
     *
     * @return the application container
     */
    public GenericContainer<?> applicationContainer() {
        return applicationContainer;
    }

    /**
     * Method to set the exporter container
     *
     * @param exporterContainer exporter container
     * @return this
     */
    public TestContext exporterContainer(GenericContainer<?> exporterContainer) {
        this.exporterContainer = exporterContainer;
        return this;
    }

    /**
     * Method to get the exporter container
     *
     * @return the exporter container
     */
    public GenericContainer<?> exporterContainer() {
        return exporterContainer;
    }

    /**
     * Method to set the base URL
     *
     * @param baseUrl baseURL
     * @return this
     */
    public TestContext baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Method to get the base URL
     *
     * @return the base URL
     */
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * Method to set the HttpClient
     *
     * @param httpClient httpClient
     * @return this
     */
    public TestContext httpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /**
     * Method to get the HttpClient
     *
     * @return the HttpClient
     */
    public HttpClient httpClient() {
        return httpClient;
    }

    /** Method to reset the test state (containers) */
    public void reset() {
        if (exporterContainer != null) {
            exporterContainer.close();
        }

        if (applicationContainer != null) {
            applicationContainer.close();
        }

        applicationContainer(null);
        exporterContainer(null);
        httpClient(null);
    }

    /** Method to dispose the test state (containers and network) */
    public void dispose() {
        if (exporterContainer != null) {
            exporterContainer.close();
            exporterContainer = null;
        }

        if (applicationContainer != null) {
            applicationContainer.close();
            applicationContainer = null;
        }

        if (network != null) {
            network.close();
            network = null;
        }

        httpClient = null;
    }
}
