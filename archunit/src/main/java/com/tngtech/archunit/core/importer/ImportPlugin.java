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

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.core.InitialConfiguration;
import com.tngtech.archunit.core.PluginLoader;

import static com.tngtech.archunit.core.PluginLoader.JavaVersion.JAVA_9;

interface ImportPlugin {
    void plugInLocationFactories(InitialConfiguration<Set<Location.Factory>> factories);

    void plugInLocationResolver(InitialConfiguration<LocationResolver> locationResolver);

    @Internal
    class Loader {
        private static final PluginLoader<ImportPlugin> pluginLoader = PluginLoader
                .forType(ImportPlugin.class)
                .ifVersionGreaterOrEqualTo(JAVA_9).load("com.tngtech.archunit.core.importer.ModuleImportPlugin")
                .fallback(new LegacyImportPlugin());

        static ImportPlugin loadForCurrentPlatform() {
            return pluginLoader.load();
        }

        private static class LegacyImportPlugin implements ImportPlugin {
            @Override
            public void plugInLocationFactories(InitialConfiguration<Set<Location.Factory>> factories) {
                factories.set(ImmutableSet.of(
                        new Location.JarFileLocationFactory(),
                        new Location.FilePathLocationFactory()
                ));
            }

            @Override
            public void plugInLocationResolver(InitialConfiguration<LocationResolver> locationResolver) {
                locationResolver.set(new LocationResolver.Legacy());
            }
        }
    }
}
