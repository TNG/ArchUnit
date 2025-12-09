/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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

import com.google.common.base.Splitter;

import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Iterables.concat;
import static com.tngtech.archunit.core.importer.UrlSource.From.iterable;
import static java.io.File.pathSeparatorChar;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toList;

class ModuleLocationResolver implements LocationResolver {
    private static final String JDK_MODULE_PATH = "jdk.module.path";
    private final FromClasspathAndUrlClassLoaders standardResolver = new FromClasspathAndUrlClassLoaders();

    @Override
    public UrlSource resolveClassPath() {
        return iterable(concat(
                standardResolver.resolveClassPath(),
                Stream.concat(ModuleFinder.ofSystem().findAll().stream(),
                              ModuleFinder.of(modulepath()).findAll().stream())
                        .flatMap(moduleReference -> moduleReference.location().stream())
                        .map(this::toUrl)
                        .collect(toList())));
    }

    private Path[] modulepath() {
        return Splitter
                .on(pathSeparatorChar)
                .omitEmptyStrings()
                .splitToList(nullToEmpty(getProperty(JDK_MODULE_PATH)))
                .stream()
                .map(Paths::get)
                .toArray(Path[]::new);
    }

    private URL toUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
