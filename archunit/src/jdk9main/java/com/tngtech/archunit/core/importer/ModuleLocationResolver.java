/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.base.Splitter;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Iterables.concat;
import static java.util.stream.Collectors.toList;

class ModuleLocationResolver implements LocationResolver {
    @Override
    public UrlSource resolveClassPath() {
        Iterable<URL> classpath = UrlSource.From.classPathSystemProperties();
        Set<ModuleReference> systemModuleReferences = ModuleFinder.ofSystem().findAll();
        Set<ModuleReference> configuredModuleReferences = ModuleFinder.of(modulepath()).findAll();
        Iterable<URL> modulepath = Stream.concat(systemModuleReferences.stream(), configuredModuleReferences.stream())
                .flatMap(moduleReference -> moduleReference.location().stream())
                .map(this::toUrl)
                .collect(toList());

        return UrlSource.From.iterable(concat(classpath, modulepath));
    }

    private Path[] modulepath() {
        String modulepathProperty = nullToEmpty(System.getProperty("jdk.module.path"));
        List<String> modulepath = Splitter.on(File.pathSeparatorChar).omitEmptyStrings().splitToList(modulepathProperty);
        Path[] result = new Path[modulepath.size()];
        for (int i = 0; i < modulepath.size(); i++) {
            result[i] = Paths.get(modulepath.get(i));
        }
        return result;
    }

    private URL toUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
