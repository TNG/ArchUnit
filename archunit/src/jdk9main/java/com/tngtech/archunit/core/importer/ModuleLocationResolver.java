/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static com.google.common.collect.Iterables.concat;
import static java.util.stream.Collectors.toList;

class ModuleLocationResolver implements LocationResolver {
    @Override
    public UrlSource resolveClassPath() {
        Iterable<URL> classpath = UrlSource.From.classPathSystemProperties();
        Iterable<URL> modulepath = ModuleFinder.ofSystem().findAll().stream()
                .flatMap(moduleReference -> moduleReference.location().stream())
                .map(this::toUrl)
                .collect(toList());

        return UrlSource.From.iterable(concat(classpath, modulepath));
    }

    private URL toUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
