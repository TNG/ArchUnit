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
package com.tngtech.archunit.core.domain;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.InitialConfiguration;
import com.tngtech.archunit.core.PluginLoader;

interface DomainPlugin {
    void plugInAnnotationValueFormatter(InitialConfiguration<Function<Object, String>> valueFormatter);

    @Internal
    class Loader {
        private static final PluginLoader<DomainPlugin> pluginLoader = PluginLoader
                .forType(DomainPlugin.class)
                .fallback(new LegacyDomainPlugin());

        static DomainPlugin loadForCurrentPlatform() {
            return pluginLoader.load();
        }

        private static class LegacyDomainPlugin implements DomainPlugin {
            @Override
            public void plugInAnnotationValueFormatter(InitialConfiguration<Function<Object, String>> valueFormatter) {
                valueFormatter.set(new LegacyValueFormatter());
            }
        }

        private static class LegacyValueFormatter implements Function<Object, String> {
            @Override
            public String apply(Object input) {
                if (!input.getClass().isArray()) {
                    return "" + input;
                }

                List<String> elemToString = new ArrayList<>();
                for (int i = 0; i < Array.getLength(input); i++) {
                    elemToString.add("" + apply(Array.get(input, i)));
                }
                return "[" + Joiner.on(", ").join(elemToString) + "]";
            }
        }
    }
}
