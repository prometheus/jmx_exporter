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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * ClassLoader for loading classes from a JAR file.
 *
 * <p>This class extends the ClassLoader and allows loading classes from a JAR file.
 */
public class JarClassLoader extends ClassLoader {

    private static final String IMPLEMENTATION_TITLE = "Implementation-Title";

    private static final String IMPLEMENTATION_VERSION = "Implementation-Version";

    private final Map<String, String> manifestMap = new HashMap<>();

    /** A map to hold the class name and its bytecode. */
    private final Map<String, byte[]> classBytes = new HashMap<>();

    /**
     * Constructor for JarClassLoader.
     *
     * @param jarPath Path to the JAR file
     * @param parent Parent classloader
     * @throws IOException If an I/O error occurs while loading the JAR
     */
    public JarClassLoader(String jarPath, ClassLoader parent) throws IOException {
        super(parent);

        loadManifest(jarPath);
        loadJar(jarPath);
    }

    /**
     * Loads the JAR file's manifest and extracts attributes.
     *
     * @param jarPath Path to the JAR file
     * @throws IOException If an I/O error occurs while loading the JAR
     */
    private void loadManifest(String jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
                    manifestMap.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }
    }

    /**
     * Loads the JAR file and extracts class bytecode.
     *
     * @param jarPath Path to the JAR file
     * @throws IOException If an I/O error occurs while loading the JAR
     */
    private void loadJar(String jarPath) throws IOException {
        try (InputStream jarStream = Files.newInputStream(Paths.get(jarPath));
                JarInputStream jis = new JarInputStream(jarStream)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    String className =
                            entry.getName()
                                    .replace('/', '.')
                                    .substring(0, entry.getName().length() - 6);

                    byte[] classData = readAllBytes(jis);
                    classBytes.put(className, classData);
                }
            }
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        byte[] bytes = classBytes.get(name);
        if (bytes != null) {
            ensurePackageDefined(name);
            Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

        return super.loadClass(name, resolve);
    }

    /**
     * Reads all bytes from the given InputStream.
     *
     * @param inputStream InputStream to read from
     * @return byte array containing all bytes read
     * @throws IOException If an I/O error occurs
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        int bufferSize = 8192;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            while ((bytesRead = inputStream.read(buffer, 0, bufferSize)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            return byteArrayOutputStream.toByteArray();
        }
    }

    /**
     * Ensures that the package for the given class name is defined.
     *
     * @param className The name of the class
     */
    private void ensurePackageDefined(String className) {
        int index = className.lastIndexOf('.');
        if (index != -1) {
            String packageName = className.substring(0, index);
            if (getPackage(packageName) == null) {
                String title = null;
                String version = null;

                if (className.startsWith("e1723a08afd7bca35570fd31a7656f59")) {
                    title = manifestMap.get(IMPLEMENTATION_TITLE);
                    version = manifestMap.get(IMPLEMENTATION_VERSION);
                }

                definePackage(packageName, null, null, null, title, version, null, null);
            }
        }
    }
}
