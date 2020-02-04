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

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

class ModuleLocationFactory implements Location.Factory {
    @Override
    public boolean supports(String scheme) {
        return ModuleLocation.SCHEME.equals(scheme);
    }

    @Override
    public Location create(URI uri) {
        return new ModuleLocation(NormalizedUri.from(uri));
    }

    private static <T> T doWithModuleReader(ModuleReference moduleReference, ModuleReaderProcessor<T> readerProcessor) {
        try (ModuleReader moduleReader = moduleReference.open()) {
            return readerProcessor.process(moduleReader);
        } catch (IOException e) {
            String message = String.format("Unexpected error while processing module %s", moduleReference);
            throw new RuntimeException(message, e);
        }
    }

    private static class ModuleLocation extends Location {
        private static final String SCHEME = "jrt";

        private final ModuleReference moduleReference;
        private final NormalizedResourceName resourceName;

        ModuleLocation(NormalizedUri uri) {
            super(uri);
            checkScheme(SCHEME, uri);
            this.moduleReference = findModuleReference(uri);
            this.resourceName = parseResourceName(uri);
        }

        ModuleLocation(ModuleReference moduleReference, NormalizedResourceName resourceName) {
            super(createUri(moduleReference, resourceName));
            this.moduleReference = moduleReference;
            this.resourceName = resourceName;
        }

        private static NormalizedUri createUri(ModuleReference moduleReference, NormalizedResourceName resourceName) {
            checkState(moduleReference.location().isPresent(),
                    "We only consider module references with location, so something went wrong here");

            return NormalizedUri.from(moduleReference.location().get() + resourceName.toAbsolutePath());
        }

        private ModuleReference findModuleReference(NormalizedUri uri) {
            String moduleName = uri.getFirstSegment();
            Optional<ModuleReference> moduleReference = ModuleFinder.ofSystem().find(moduleName);
            checkState(moduleReference.isPresent(), "Couldn't find module %s of URI %s", moduleName, uri);
            return moduleReference.get();
        }

        private NormalizedResourceName parseResourceName(NormalizedUri uri) {
            return NormalizedResourceName.from(uri.getTailSegments());
        }

        @Override
        public boolean isJar() {
            return false;
        }

        @Override
        public boolean isArchive() {
            return true;
        }

        @Override
        Iterable<NormalizedResourceName> iterateEntries() {
            return doWithModuleReader(moduleReference, moduleReader -> moduleReader.list()
                    .filter(resourceName::isStartOf)
                    .map(NormalizedResourceName::from)
                    .collect(toList()));
        }

        @Override
        ClassFileSource asClassFileSource(ImportOptions importOptions) {
            if (!importOptions.include(this)) {
                return Collections::emptyListIterator;
            }
            return new ModuleClassFileSource(moduleReference, resourceName, importOptions);
        }
    }

    private static class ModuleClassFileSource implements ClassFileSource {
        private final Stream<ClassFileLocation> locations;

        ModuleClassFileSource(
                ModuleReference moduleReference,
                NormalizedResourceName resourceName,
                ImportOptions importOptions) {

            Set<String> entries = loadEntries(moduleReference, resourceName);
            locations = entries.stream()
                    .map(entry -> new ModuleClassFileLocation(moduleReference, entry))
                    .filter(classFileLocation -> classFileLocation.isIncludedBy(importOptions))
                    .map(Function.identity()); // thanks Java type system :-(
        }

        private Set<String> loadEntries(ModuleReference moduleReference, NormalizedResourceName resourceName) {
            return doWithModuleReader(moduleReference, moduleReader -> moduleReader.list()
                    .filter(resourceName::isStartOf)
                    .filter(FileToImport::isRelevant)
                    .map(entry -> "/" + entry)
                    .collect(toSet()));
        }

        @Override
        public Iterator<ClassFileLocation> iterator() {
            return locations.iterator();
        }
    }

    private static class ModuleClassFileLocation implements ClassFileLocation {
        private final ModuleReference moduleReference;
        private final NormalizedResourceName entry;
        private final ModuleLocation location;

        ModuleClassFileLocation(ModuleReference moduleReference, String entry) {
            this(moduleReference, NormalizedResourceName.from(entry));
        }

        ModuleClassFileLocation(ModuleReference moduleReference, NormalizedResourceName entry) {
            this.moduleReference = moduleReference;
            this.entry = entry;
            location = new ModuleLocation(moduleReference, entry);
        }

        @Override
        public InputStream openStream() {
            return doWithModuleReader(moduleReference, moduleReader ->
                    moduleReader.open(entry.toString()).orElseThrow(() -> new IllegalStateException(
                            String.format("Entry %s parsed from JRT location %s could not be opened. This is most likely a bug.", entry, location))));
        }

        @Override
        public URI getUri() {
            return location.asURI();
        }

        boolean isIncludedBy(ImportOptions importOptions) {
            return importOptions.include(location);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{uri=" + getUri() + '}';
        }
    }

    @FunctionalInterface
    private interface ModuleReaderProcessor<T> {
        T process(ModuleReader reader) throws IOException;
    }
}
