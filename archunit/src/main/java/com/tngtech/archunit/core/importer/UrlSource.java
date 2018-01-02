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
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

interface UrlSource extends Iterable<URL> {
    @Internal
    class From {
        private static final Logger LOG = LoggerFactory.getLogger(From.class);

        static UrlSource iterable(final Iterable<URL> iterable) {
            return new UrlSource() {
                @Override
                public Iterator<URL> iterator() {
                    return iterable.iterator();
                }
            };
        }

        static UrlSource classPathSystemProperty() {
            String classPathProperty = System.getProperty("java.class.path");
            List<URL> urls = new ArrayList<>();
            for (String path : classPathProperty.split(File.pathSeparator)) {
                urls.addAll(parseClassPathEntry(path).asSet());
            }
            LOG.debug("Found URLs on classpath: {}", urls);
            return iterable(urls);
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
                return Optional.of(new URL(protocol + "://" + path));
            } catch (MalformedURLException e) {
                LOG.warn("Cannot parse URL from path " + path, e);
                return Optional.absent();
            }
        }
    }
}
