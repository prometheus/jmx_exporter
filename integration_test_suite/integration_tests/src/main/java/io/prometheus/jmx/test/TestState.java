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

public class TestState {

    private Network network;
    private GenericContainer<?> applicationContainer;
    private GenericContainer<?> exporterContainer;
    private String baseUrl;
    private HttpClient httpClient;

    public TestState() {
        // DO NOTHING
    }

    public TestState network(Network network) {
        this.network = network;
        return this;
    }

    public Network network() {
        return network;
    }

    public TestState applicationContainer(GenericContainer<?> applicationContainer) {
        this.applicationContainer = applicationContainer;
        return this;
    }

    public GenericContainer<?> applicationContainer() {
        return applicationContainer;
    }

    public TestState exporterContainer(GenericContainer<?> exporterContainer) {
        this.exporterContainer = exporterContainer;
        return this;
    }

    public GenericContainer<?> exporterContainer() {
        return exporterContainer;
    }

    public TestState baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public TestState httpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public HttpClient httpClient() {
        return httpClient;
    }

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
