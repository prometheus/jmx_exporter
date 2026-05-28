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
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Isolating classloader that loads classes from a JAR file.
 *
 * <p>Extends the standard classloader to load classes directly from a JAR file into memory.
 * This allows multiple versions of the same library to coexist in different classloaders,
 * enabling isolation between JMX exporter instances.
 *
 * <p>Class bytecode is loaded lazily on first request rather than eagerly at construction
 * time. Package metadata is preserved, including implementation title and version from the
 * JAR manifest.
 *
 * <p><b>Intentional deviation from standard delegation:</b> This classloader overrides
 * {@link #loadClass(String, boolean)} instead of {@link #findClass(String)} to implement a
 * <i>self-first</i> (child-first) delegation strategy. The standard Java classloading contract
 * (parent-first) is deliberately inverted to ensure that classes from the isolated JAR take
 * priority over any versions of the same classes that may exist on the system classpath. This
 * is the core mechanism of the isolator pattern.
 *
 * <p>The delegation order is:
 * <ol>
 *   <li>Check if the class is already loaded ({@link #findLoadedClass(String)})
 *   <li>Load the class bytecode from the JAR on demand
 *   <li>Fall back to the standard parent-first chain ({@code super.loadClass()})
 * </ol>
 *
 * <p>Each exporter JAR is fully shaded under the
 * {@code e1723a08afd7bca35570fd31a7656f59.} prefix, so all dependency classes are
 * namespace-scoped to that JAR. The self-first strategy ensures the exact version packaged
 * inside each JAR is used, preventing {@link ClassCastException} and other version conflicts
 * when multiple exporter instances run in the same JVM.
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
     * Path to the JAR file from which classes are loaded.
     */
    private final String jarPath;

    /**
     * Manifest attributes extracted from the JAR.
     */
    private final Map<String, String> manifestMap = new HashMap<>();

    /**
     * Cache of class names to their bytecode, populated lazily on first load.
     */
    private final Map<String, byte[]> classBytes = new HashMap<>();

    /**
     * Constructs a classloader that loads classes from the specified JAR file.
     *
     * <p>Reads the JAR manifest eagerly. Class bytecode is loaded lazily on demand when
     * {@link #loadClass(String, boolean)} is called.
     *
     * @param jarPath path to the JAR file, must not be {@code null} and must exist
     * @param parent  the parent classloader for delegation, must not be {@code null}
     * @throws IOException if the JAR manifest cannot be read or parsed
     */
    public JarClassLoader(String jarPath, ClassLoader parent) throws IOException {
        super(parent);
        this.jarPath = jarPath;

        loadManifest(jarPath);
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
     * Loads the bytecode for the given class from the JAR file.
     *
     * <p>Checks the cache first. On cache miss, opens the JAR file, finds the corresponding
     * entry, reads its bytes, caches them, and returns the result.
     *
     * @param name the fully qualified class name
     * @return the class bytecode, or {@code null} if the class is not in the JAR
     * @throws ClassNotFoundException if an I/O error occurs reading the JAR
     */
    private byte[] loadClassBytes(String name) throws ClassNotFoundException {
        byte[] cached = classBytes.get(name);
        if (cached != null) {
            return cached;
        }

        String entryName = name.replace('.', '/') + ".class";
        try (JarFile jarFile = new JarFile(jarPath)) {
            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                return null;
            }
            byte[] bytes = readAllBytes(jarFile.getInputStream(entry));
            classBytes.put(name, bytes);
            return bytes;
        } catch (IOException e) {
            throw new ClassNotFoundException("Failed to load class " + name + " from " + jarPath, e);
        }
    }

    /**
     * Loads a class with self-first (child-first) delegation.
     *
     * <p><b>Intentional override of {@code loadClass()} instead of {@code findClass()}.</b>
     * Standard Java classloaders follow a parent-first delegation pattern: check parent, then
     * self. This classloader inverts that order to ensure classes from the isolated JAR take
     * priority over any versions on the system classpath.
     *
     * <p>Delegation order:
     * <ol>
     *   <li>Check if the class is already loaded via {@link #findLoadedClass(String)}</li>
     *   <li>Load the class bytecode from the JAR on demand</li>
     *   <li>Fall back to the standard parent-first chain via {@code super.loadClass()}</li>
     * </ol>
     *
     * <p>The parent chain is preserved for classes not present in the JAR, such as
     * {@code java.*} and other JVM platform classes, which should never be loaded from
     * the isolated JAR.
     *
     * @param name    the fully qualified class name
     * @param resolve whether to resolve (link) the class
     * @return the loaded {@link Class}
     * @throws ClassNotFoundException if the class cannot be found or an I/O error occurs
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        byte[] bytes = loadClassBytes(name);
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
