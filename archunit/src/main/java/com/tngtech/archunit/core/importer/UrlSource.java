/*
 * Copyright 2018 TNG Technology Consulting GmbH
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.net.UrlEscapers;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.ArchUnitException.LocationException;
import com.tngtech.archunit.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.importer.Location.toURI;

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
            return iterable(ImmutableList.<URL>builder()
                    .addAll(findUrlsForClassPathProperty(BOOT_CLASS_PATH_PROPERTY_NAME))
                    .addAll(findUrlsForClassPathProperty(CLASS_PATH_PROPERTY_NAME))
                    .build());
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
            return path.endsWith(".jar") ? newJarUri(path) : newFileUri(path);
        }

        private static Optional<URL> newFileUri(String path) {
            path = path.endsWith("/") ? path : path + "/";
            return newUrl("file", path);
        }

        private static Optional<URL> newJarUri(String path) {
            return newUrl("jar:file", path + "!/");
        }

        private static Optional<URL> newUrl(String protocol, String path) {
            try {
                return Optional.of(new URL(protocol + "://" + UrlEscapers.urlFragmentEscaper().escape(path)));
            } catch (MalformedURLException e) {
                LOG.warn("Cannot parse URL from path " + path, e);
                return Optional.absent();
            } catch (IllegalArgumentException e){
                LOG.warn("Cannot escape fragments from path " + path, e);
                return Optional.absent();
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
