/*
 * https://raw.githubusercontent.com/dhoard/testcontainers-java/main/core/src/main/java/org/testcontainers/utility/DockerLoggerFactory.java
 */
package org.testcontainers.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom DockerLoggerFactory to manipulate Docker image name
 */
@SuppressWarnings("unused")
public final class DockerLoggerFactory {

    public static Logger getLogger(String dockerImageName) {
        final String abbreviatedName;
        if (dockerImageName.contains("@sha256")) {
            abbreviatedName = dockerImageName.substring(0, dockerImageName.indexOf("@sha256") + 14) + "...";
        } else {
            abbreviatedName = dockerImageName;
        }

        return LoggerFactory.getLogger(abbreviatedName);
    }
}