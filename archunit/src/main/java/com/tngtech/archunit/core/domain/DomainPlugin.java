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
package com.tngtech.archunit.core.domain;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.core.InitialConfiguration;
import com.tngtech.archunit.core.PluginLoader;

import static com.tngtech.archunit.core.PluginLoader.JavaVersion.JAVA_14;
import static com.tngtech.archunit.core.PluginLoader.JavaVersion.JAVA_9;

interface DomainPlugin {
    void plugInAnnotationPropertiesFormatter(InitialConfiguration<AnnotationPropertiesFormatter> valueFormatter);

    @Internal
    class Loader {
        private static final PluginLoader<DomainPlugin> pluginLoader = PluginLoader
                .forType(DomainPlugin.class)
                .ifVersionGreaterOrEqualTo(JAVA_9).load("com.tngtech.archunit.core.domain.Java9DomainPlugin")
                .ifVersionGreaterOrEqualTo(JAVA_14).load("com.tngtech.archunit.core.domain.Java14DomainPlugin")
                .fallback(new LegacyDomainPlugin());

        static DomainPlugin loadForCurrentPlatform() {
            return pluginLoader.load();
        }

        private static class LegacyDomainPlugin implements DomainPlugin {
            @Override
            public void plugInAnnotationPropertiesFormatter(InitialConfiguration<AnnotationPropertiesFormatter> valueFormatter) {
                valueFormatter.set(AnnotationPropertiesFormatter.configure()
                        .formattingArraysWithSquareBrackets()
                        .formattingTypesToString()
                        .build());
            }
        }
    }
}
