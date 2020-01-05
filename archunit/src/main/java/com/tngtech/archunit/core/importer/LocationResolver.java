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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.Internal;

interface LocationResolver {
    UrlSource resolveClassPath();

    @Internal
    class Legacy implements LocationResolver {
        @Override
        public UrlSource resolveClassPath() {
            ImmutableList.Builder<URL> result = ImmutableList.builder();
            for (URLClassLoader loader : findAllUrlClassLoadersInContext()) {
                result.addAll(ImmutableList.copyOf(loader.getURLs()));
            }
            result.addAll(UrlSource.From.classPathSystemProperties());
            return UrlSource.From.iterable(result.build());
        }

        private static Set<URLClassLoader> findAllUrlClassLoadersInContext() {
            return ImmutableSet.<URLClassLoader>builder()
                    .addAll(findUrlClassLoadersInHierarchy(Thread.currentThread().getContextClassLoader()))
                    .addAll(findUrlClassLoadersInHierarchy(UrlSource.class.getClassLoader()))
                    .build();
        }

        private static Set<URLClassLoader> findUrlClassLoadersInHierarchy(ClassLoader loader) {
            Set<URLClassLoader> result = new HashSet<>();
            while (loader != null) {
                if (loader instanceof URLClassLoader) {
                    result.add((URLClassLoader) loader);
                }
                loader = loader.getParent();
            }
            return result;
        }
    }
}
