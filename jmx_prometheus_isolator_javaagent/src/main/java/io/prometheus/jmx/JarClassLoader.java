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
 * Isolating classloader that loads classes from a JAR file.
 *
 * <p>Extends the standard classloader to load classes directly from a JAR file into memory.
 * This allows multiple versions of the same library to coexist in different classloaders,
 * enabling isolation between JMX exporter instances.
 *
 * <p>The classloader reads all .class files from the JAR at construction time and stores them
 * in memory. Package metadata is preserved, including implementation title and version from
 * the JAR manifest.
 *
 * <p>Thread-safety: This class is thread-safe. Class loading operations are synchronized by
 * the parent ClassLoader.
 */
public class JarClassLoader extends ClassLoader {

    /**
     * Manifest attribute name for implementation title.
     */
    private static final String IMPLEMENTATION_TITLE = "Implementation-Title";

    /**
     * Manifest attribute name for implementation version.
     */
    private static final String IMPLEMENTATION_VERSION = "Implementation-Version";

    /**
     * Manifest attributes extracted from the JAR.
     */
    private final Map<String, String> manifestMap = new HashMap<>();

    /**
     * Map of class names to their bytecode.
     */
    private final Map<String, byte[]> classBytes = new HashMap<>();

    /**
     * Constructs a classloader that loads classes from the specified JAR file.
     *
     * <p>Reads the JAR manifest and all class files into memory. Classes are loaded lazily
     * on demand.
     *
     * @param jarPath path to the JAR file, must not be {@code null} and must exist
     * @param parent the parent classloader for delegation, must not be {@code null}
     * @throws IOException if the JAR cannot be read or parsed
     */
    public JarClassLoader(String jarPath, ClassLoader parent) throws IOException {
        super(parent);

        loadManifest(jarPath);
        loadJar(jarPath);
    }

    /**
     * Loads the JAR file's manifest and extracts attributes.
     *
     * <p>Extracts implementation title and version from the manifest for package metadata.
     *
     * @param jarPath path to the JAR file
     * @throws IOException if the JAR cannot be read
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
     * Loads the JAR file and extracts class bytecode into memory.
     *
     * <p>All .class files found in the JAR are read and stored in the classBytes map for lazy
     * loading.
     *
     * @param jarPath path to the JAR file
     * @throws IOException if the JAR cannot be read
     */
    private void loadJar(String jarPath) throws IOException {
        try (InputStream jarStream = Files.newInputStream(Paths.get(jarPath));
                JarInputStream jis = new JarInputStream(jarStream)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);

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
     * Reads all bytes from an input stream into a byte array.
     *
     * @param inputStream the input stream to read from
     * @return byte array containing all bytes from the stream
     * @throws IOException if an I/O error occurs
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
     * Ensures that the package for a class is defined before loading.
     *
     * <p>For classes with the shading prefix, the package is defined with implementation
     * title and version from the JAR manifest.
     *
     * @param className the fully qualified class name
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
