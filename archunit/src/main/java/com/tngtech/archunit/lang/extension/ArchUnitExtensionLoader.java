/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import com.google.common.base.Supplier;

import static com.google.common.base.Suppliers.memoize;

class ArchUnitExtensionLoader {
    private final Supplier<Iterable<ArchUnitExtension>> extensions = memoize(new Supplier<Iterable<ArchUnitExtension>>() {
        @Override
        public Iterable<ArchUnitExtension> get() {
            ServiceLoader<ArchUnitExtension> extensions = ServiceLoader.load(ArchUnitExtension.class);
            checkIdentifiersNonNull(extensions);
            checkIdentifiersUnique(extensions);
            return extensions;
        }

        private void checkIdentifiersNonNull(Iterable<ArchUnitExtension> extensions) {
            for (ArchUnitExtension extension : extensions) {
                if (extension.getUniqueIdentifier() == null) {
                    throw new ExtensionLoadingException(String.format(
                            "Failed to load %s: Extension identifier must not be null", extension.getClass().getName()));
                }
            }
        }

        private void checkIdentifiersUnique(Iterable<ArchUnitExtension> extensions) {
            Map<String, ArchUnitExtension> alreadyPresent = new HashMap<>();
            for (ArchUnitExtension extension : extensions) {
                if (alreadyPresent.containsKey(extension.getUniqueIdentifier())) {
                    throw new ExtensionLoadingException(String.format(
                            "Failed to load %s: Extension identifiers must be unique, but %s also has identifier '%s'",
                            extension.getClass().getName(),
                            alreadyPresent.get(extension.getUniqueIdentifier()).getClass().getName(),
                            extension.getUniqueIdentifier()));
                }
                alreadyPresent.put(extension.getUniqueIdentifier(), extension);
            }
        }
    });

    Iterable<ArchUnitExtension> getAll() {
        return extensions.get();
    }
}
