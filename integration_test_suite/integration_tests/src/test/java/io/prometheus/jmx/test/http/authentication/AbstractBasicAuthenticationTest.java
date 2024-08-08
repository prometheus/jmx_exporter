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

package io.prometheus.jmx.test.http.authentication;

import static io.prometheus.jmx.test.support.http.HttpResponseAssertions.assertHttpResponseCode;

import io.prometheus.jmx.test.common.AbstractExporterTest;
import io.prometheus.jmx.test.common.AuthenticationCredentials;
import io.prometheus.jmx.test.common.ExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpBasicAuthenticationCredentials;
import io.prometheus.jmx.test.support.http.HttpHealthyRequest;
import io.prometheus.jmx.test.support.http.HttpMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpOpenMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusProtobufMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import org.antublue.verifyica.api.ArgumentContext;
import org.antublue.verifyica.api.Verifyica;

public abstract class AbstractBasicAuthenticationTest extends AbstractExporterTest {

    protected static final String VALID_USERNAME = "Prometheus";

    protected static final String VALID_PASSWORD = "secret";

    protected static final String[] INVALID_USERNAMES =
            new String[] {"prometheus", "bad", "", null};

    protected static final String[] INVALID_PASSWORDS = new String[] {"Secret", "bad", "", null};

    /**
     * Method to create a Collection of AuthenticationCredentials
     *
     * @return a Collection of AuthenticationCredentials
     */
    public static Collection<AuthenticationCredentials> getAuthenticationCredentials() {
        Collection<AuthenticationCredentials> collection = new ArrayList<>();
        collection.add(AuthenticationCredentials.of(VALID_USERNAME, VALID_PASSWORD, true));

        for (String username : INVALID_USERNAMES) {
            for (String password : INVALID_PASSWORDS) {
                collection.add(AuthenticationCredentials.of(username, password, false));
            }
        }

        return collection;
    }

    @Verifyica.Test
    public void testHealthy(ArgumentContext argumentContext) {
        ExporterTestEnvironment exporterTestEnvironment =
                argumentContext.getTestArgument(ExporterTestEnvironment.class).getPayload();

        getAuthenticationCredentials()
                .forEach(
                        authenticationTestArguments -> {
                            final AtomicInteger code =
                                    new AtomicInteger(
                                            authenticationTestArguments.isValid()
                                                    ? HttpResponse.OK
                                                    : HttpResponse.UNAUTHORIZED);
                            new HttpHealthyRequest()
                                    .credentials(
                                            new HttpBasicAuthenticationCredentials(
                                                    authenticationTestArguments.getUsername(),
                                                    authenticationTestArguments.getPassword()))
                                    .send(exporterTestEnvironment.getHttpClient())
                                    .accept(
                                            response ->
                                                    assertHttpResponseCode(response, code.get()));
                        });
    }

    @Verifyica.Test
    public void testMetrics(ArgumentContext argumentContext) {
        ExporterTestEnvironment exporterTestEnvironment =
                argumentContext.getTestArgument(ExporterTestEnvironment.class).getPayload();

        getAuthenticationCredentials()
                .forEach(
                        authenticationCredentials -> {
                            final AtomicInteger code =
                                    new AtomicInteger(
                                            authenticationCredentials.isValid()
                                                    ? HttpResponse.OK
                                                    : HttpResponse.UNAUTHORIZED);
                            new HttpMetricsRequest()
                                    .credentials(
                                            new HttpBasicAuthenticationCredentials(
                                                    authenticationCredentials.getUsername(),
                                                    authenticationCredentials.getPassword()))
                                    .send(exporterTestEnvironment.getHttpClient())
                                    .accept(
                                            response -> {
                                                assertHttpResponseCode(response, code.get());
                                                if (code.get() == HttpResponse.OK) {
                                                    accept(exporterTestEnvironment, response);
                                                }
                                            });
                        });
    }

    @Verifyica.Test
    public void testMetricsOpenMetricsFormat(ArgumentContext argumentContext) {
        ExporterTestEnvironment exporterTestEnvironment =
                argumentContext.getTestArgument(ExporterTestEnvironment.class).getPayload();

        getAuthenticationCredentials()
                .forEach(
                        authenticationCredentials -> {
                            final AtomicInteger code =
                                    new AtomicInteger(
                                            authenticationCredentials.isValid()
                                                    ? HttpResponse.OK
                                                    : HttpResponse.UNAUTHORIZED);
                            new HttpOpenMetricsRequest()
                                    .credentials(
                                            new HttpBasicAuthenticationCredentials(
                                                    authenticationCredentials.getUsername(),
                                                    authenticationCredentials.getPassword()))
                                    .send(exporterTestEnvironment.getHttpClient())
                                    .accept(
                                            response -> {
                                                assertHttpResponseCode(response, code.get());
                                                if (code.get() == HttpResponse.OK) {
                                                    accept(exporterTestEnvironment, response);
                                                }
                                            });
                        });
    }

    @Verifyica.Test
    public void testMetricsPrometheusFormat(ArgumentContext argumentContext) {
        ExporterTestEnvironment exporterTestEnvironment =
                argumentContext.getTestArgument(ExporterTestEnvironment.class).getPayload();

        getAuthenticationCredentials()
                .forEach(
                        authenticationCredentials -> {
                            final AtomicInteger code =
                                    new AtomicInteger(
                                            authenticationCredentials.isValid()
                                                    ? HttpResponse.OK
                                                    : HttpResponse.UNAUTHORIZED);
                            new HttpPrometheusMetricsRequest()
                                    .credentials(
                                            new HttpBasicAuthenticationCredentials(
                                                    authenticationCredentials.getUsername(),
                                                    authenticationCredentials.getPassword()))
                                    .send(exporterTestEnvironment.getHttpClient())
                                    .accept(
                                            response -> {
                                                assertHttpResponseCode(response, code.get());
                                                if (code.get() == HttpResponse.OK) {
                                                    accept(exporterTestEnvironment, response);
                                                }
                                            });
                        });
    }

    @Verifyica.Test
    public void testMetricsPrometheusProtobufFormat(ArgumentContext argumentContext) {
        ExporterTestEnvironment exporterTestEnvironment =
                argumentContext.getTestArgument(ExporterTestEnvironment.class).getPayload();

        getAuthenticationCredentials()
                .forEach(
                        authenticationCredentials -> {
                            final AtomicInteger code =
                                    new AtomicInteger(
                                            authenticationCredentials.isValid()
                                                    ? HttpResponse.OK
                                                    : HttpResponse.UNAUTHORIZED);
                            new HttpPrometheusProtobufMetricsRequest()
                                    .credentials(
                                            new HttpBasicAuthenticationCredentials(
                                                    authenticationCredentials.getUsername(),
                                                    authenticationCredentials.getPassword()))
                                    .send(exporterTestEnvironment.getHttpClient())
                                    .accept(
                                            response -> {
                                                assertHttpResponseCode(response, code.get());
                                                if (code.get() == HttpResponse.OK) {
                                                    accept(exporterTestEnvironment, response);
                                                }
                                            });
                        });
    }
}
