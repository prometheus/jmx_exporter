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

package io.prometheus.jmx;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Check tomcat path
 *
 * <pre>
 * Catalina:j2eeType=Servlet,WebModule=//localhost/host-manager,name=HTMLHostManager,J2EEApplication=none,J2EEServer=none
 * </pre>
 *
 * See <a
 * href="https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html">java.util.regex
 * Class Pattern (Java Platform SE 7)</a>
 *
 * <p>or
 *
 * <p><a
 * href=""https://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java>Regular
 * expression to match URLs in Java</a>
 */
public class TomcatPatternCheckTest {

    private static final Pattern VALID_TOMCAT_PATH =
            Pattern.compile("//([-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])");

    private static final Pattern VALID_SERVLET_NAME = Pattern.compile("([-a-zA-Z0-9+/$%~_-|!.]*)");

    private static final Pattern VALID_WEBMODULE =
            Pattern.compile(
                    "^.*j2eeType=Servlet,WebModule=//([-a-zA-Z0-9+&@#/%?=~_|!:.,;]*[-a-zA-Z0-9+&@#/%=~_|]),name=([-a-zA-Z0-9+/$%~_-|!.]*),J2EEApplication=none,J2EEServer=none.*$");

    public static boolean validateTomcatPath(String identifier) {
        return VALID_TOMCAT_PATH.matcher(identifier).matches();
    }

    public static boolean validateServletName(String identifier) {
        return VALID_SERVLET_NAME.matcher(identifier).matches();
    }

    public static boolean validateWebModule(String identifier) {
        return VALID_WEBMODULE.matcher(identifier).matches();
    }

    @Test
    public void testServletName() {
        assertThat(validateServletName("C")).isTrue();
        assertThat(validateServletName("Cc")).isTrue();
        assertThat(validateServletName("C$c")).isTrue();
        assertThat(validateServletName("C9")).isTrue();
        assertThat(validateServletName("host-manager")).isTrue();
        assertThat(validateServletName("a.C")).isTrue();
        assertThat(validateServletName(".C")).isTrue();
        assertThat(validateServletName("prom_app_metrics")).isTrue();
    }

    @Test
    public void validateTomcatPath() {
        assertThat(validateTomcatPath("//localhost/")).isTrue();
        assertThat(validateTomcatPath("//localhost/docs/")).isTrue();
        assertThat(validateTomcatPath("//www.example.com/prom-metric/")).isTrue();
        assertThat(validateTomcatPath("//www.example.com/prom_metric+tomcat/")).isTrue();
        // no tomcat path, but a validate url?
        assertThat(validateTomcatPath("//www.example.com:443;jsessionid=sajakjda/prom-metric/"))
                .isTrue();
        assertThat(validateTomcatPath("//localhost/$docs/"))
                .withFailMessage("cannot include $")
                .isFalse();
        assertThat(validateTomcatPath("//localhost/docs()/"))
                .withFailMessage("cannot include ()")
                .isFalse();
    }

    @Test
    public void testWebModule() {
        assertThat(
                        validateWebModule(
                                "Catalina:j2eeType=Servlet,WebModule=//localhost/host-manager,name=HTMLHostManager,J2EEApplication=none,J2EEServer=none"))
                .isTrue();
    }
}
