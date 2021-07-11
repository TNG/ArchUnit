/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.ArchUnitException.LocationException;
import com.tngtech.archunit.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Iterables.concat;
import static com.tngtech.archunit.core.importer.Location.toURI;
import static java.util.Collections.emptySet;
import static java.util.jar.Attributes.Name.CLASS_PATH;

interface UrlSource extends Iterable<URL> {
    @Internal
    class From {
        private static final Logger LOG = LoggerFactory.getLogger(From.class);

        private static final String CLASS_PATH_PROPERTY_NAME = "java.class.path";
        private static final String BOOT_CLASS_PATH_PROPERTY_NAME = "sun.boot.class.path";

        static UrlSource iterable(Iterable<URL> urls) {
            final Iterable<URL> uniqueUrls = unique(urls);
            return new UrlSource() {
                @Override
                public Iterator<URL> iterator() {
                    return uniqueUrls.iterator();
                }

                @Override
                public String toString() {
                    return String.valueOf(uniqueUrls);
                }
            };
        }

        private static Iterable<URL> unique(Iterable<URL> urls) {
            Set<URI> unique = FluentIterable.from(urls).transform(URL_TO_URI).toSet();
            return FluentIterable.from(unique).transform(URI_TO_URL);
        }

        static UrlSource classPathSystemProperties() {
            List<URL> directlySpecifiedAsProperties = ImmutableList.<URL>builder()
                    .addAll(findUrlsForClassPathProperty(BOOT_CLASS_PATH_PROPERTY_NAME))
                    .addAll(findUrlsForClassPathProperty(CLASS_PATH_PROPERTY_NAME))
                    .build();
            Iterable<URL> transitivelySpecifiedThroughManifest = readClasspathEntriesFromManifests(directlySpecifiedAsProperties);
            return iterable(concat(directlySpecifiedAsProperties, transitivelySpecifiedThroughManifest));
        }

        private static Iterable<URL> readClasspathEntriesFromManifests(List<URL> urls) {
            Set<URI> result = new HashSet<>();
            readClasspathUriEntriesFromManifests(result, FluentIterable.from(urls).transform(URL_TO_URI));
            return FluentIterable.from(result).transform(URI_TO_URL);
        }

        // Use URI because of better equals / hashcode
        private static void readClasspathUriEntriesFromManifests(Set<URI> result, Iterable<URI> urls) {
            for (URI url : urls) {
                if (url.getScheme().equals("jar")) {
                    Set<URI> manifestUris = readClasspathEntriesFromManifest(url);
                    Set<URI> unknownSoFar = ImmutableSet.copyOf(Sets.difference(manifestUris, result));
                    result.addAll(unknownSoFar);
                    readClasspathUriEntriesFromManifests(result, unknownSoFar);
                }
            }
        }

        private static Set<URI> readClasspathEntriesFromManifest(URI url) {
            Optional<Path> jarPath = findParentPathOf(url);
            if (!jarPath.isPresent()) {
                return emptySet();
            }

            Set<URI> result = new HashSet<>();
            for (String classpathEntry : Splitter.on(" ").omitEmptyStrings().split(readManifestClasspath(url))) {
                result.addAll(parseManifestClasspathEntry(jarPath.get(), classpathEntry).asSet());
            }
            return result;
        }

        private static Optional<Path> findParentPathOf(URI uri) {
            try {
                return Optional.ofNullable(Paths.get(ensureFileUrl(uri).toURI()).getParent());
            } catch (Exception e) {
                LOG.warn("Could not find parent folder for " + uri, e);
                return Optional.empty();
            }
        }

        private static URL ensureFileUrl(URI url) throws IOException {
            return ((JarURLConnection) url.toURL().openConnection()).getJarFileURL();
        }

        private static String readManifestClasspath(URI uri) {
            try {
                String result = (String) ((JarURLConnection) uri.toURL().openConnection()).getMainAttributes().get(CLASS_PATH);
                return nullToEmpty(result);
            } catch (Exception e) {
                return "";
            }
        }

        private static Optional<URI> parseManifestClasspathEntry(Path parent, String classpathEntry) {
            if (isUrl(classpathEntry)) {
                return parseUrl(parent, classpathEntry);
            } else {
                return parsePath(parent, classpathEntry);
            }
        }

        private static boolean isUrl(String classpathEntry) {
            return classpathEntry.startsWith("file:") || classpathEntry.startsWith("jar:");
        }

        private static Optional<URI> parseUrl(Path parent, String classpathUrlEntry) {
            try {
                return Optional.of(convertToJarUrlIfNecessary(parent.toUri().resolve(URI.create(classpathUrlEntry).getRawSchemeSpecificPart())));
            } catch (Exception e) {
                LOG.warn("Cannot parse URL classpath entry " + classpathUrlEntry, e);
                return Optional.empty();
            }
        }

        private static Optional<URI> parsePath(Path parent, String classpathFilePathEntry) {
            try {
                Path path = Paths.get(classpathFilePathEntry);
                if (!path.isAbsolute()) {
                    path = parent.resolve(path);
                }
                return Optional.of(convertToJarUrlIfNecessary(path.toUri()));
            } catch (Exception e) {
                LOG.warn("Cannot parse file path classpath entry " + classpathFilePathEntry, e);
                return Optional.empty();
            }
        }

        private static URI convertToJarUrlIfNecessary(URI uri) {
            if (uri.toString().endsWith(".jar")) {
                return URI.create("jar:" + uri + "!/");
            }
            return uri;
        }

        private static List<URL> findUrlsForClassPathProperty(String propertyName) {
            String classPathProperty = System.getProperty(propertyName, "");
            List<URL> urls = new ArrayList<>();
            for (String path : Splitter.on(File.pathSeparator).omitEmptyStrings().split(classPathProperty)) {
                urls.addAll(parseClassPathEntry(path).asSet());
            }
            LOG.debug("Found URLs on {}: {}", propertyName, urls);
            return urls;
        }

        private static Optional<URL> parseClassPathEntry(String path) {
            return path.endsWith(".jar") ? newJarUrl(path) : newFileUri(path);
        }

        private static Optional<URL> newFileUri(String path) {
            path = path.endsWith(File.separator) || path.endsWith(".class") ? path : path + File.separator;
            try {
                return Optional.of(Paths.get(path).toUri().toURL());
            } catch (MalformedURLException e) {
                LOG.warn("Cannot parse URL from path " + path, e);
                return Optional.empty();
            } catch (InvalidPathException e) {
                Optional<URL> fallback = tryResolvePathFromUrl(path);
                if (!fallback.isPresent()) {
                    LOG.warn("Cannot parse URL from path " + path, e);
                }
                return fallback;
            }
        }

        /*
         * Eclipse on Windows sometimes adds URL paths like '/C:/foo/bar' to the classpath property,
         * as well as regular paths like 'C:\foo\bar'.
         * We will try to be resilient and parse as much as possible, but convert the url to path
         * and back to make sure it represents a valid file path in the end.
         */
        private static Optional<URL> tryResolvePathFromUrl(String path) {
            try {
                return Optional.of(Paths.get(new URL("file:" + path).toURI()).toUri().toURL());
            } catch (MalformedURLException | URISyntaxException | InvalidPathException e) {
                return Optional.empty();
            }
        }

        private static Optional<URL> newJarUrl(String path) {
            Optional<URL> fileUri = newFileUri(path);

            try {
                return fileUri.isPresent() ? Optional.of(new URL("jar:" + fileUri.get() + "!/")) : Optional.<URL>empty();
            } catch (MalformedURLException e) {
                LOG.warn("Cannot parse URL from path " + path, e);
                return Optional.empty();
            }
        }

        private static final Function<URL, URI> URL_TO_URI = new Function<URL, URI>() {
            @Override
            public URI apply(URL input) {
                return toURI(input);
            }
        };

        private static final Function<URI, URL> URI_TO_URL = new Function<URI, URL>() {
            @Override
            public URL apply(URI input) {
                try {
                    return input.toURL();
                } catch (MalformedURLException e) {
                    throw new LocationException(e);
                }
            }
        };
    }
}
