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

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * A collection of {@link ImportOption} to filter class locations. All supplied {@link ImportOption}s will be joined
 * with <b>AND</b>, i.e. only {@link Location}s that are accepted by <b>all</b> {@link ImportOption}s
 * will be imported.
 */
public final class ImportOptions {
    private final Set<ImportOption> options;

    @PublicAPI(usage = ACCESS)
    public ImportOptions() {
        this(Collections.<ImportOption>emptySet());
    }

    private ImportOptions(Set<ImportOption> options) {
        this.options = checkNotNull(options);
    }

    /**
     * @param option An {@link ImportOption} to evaluate on {@link Location}s of class files
     * @return self to add further {@link ImportOption}s in a fluent way
     */
    @PublicAPI(usage = ACCESS)
    public ImportOptions with(ImportOption option) {
        return new ImportOptions(ImmutableSet.<ImportOption>builder().addAll(options).add(option).build());
    }

    boolean include(Location location) {
        for (ImportOption option : options) {
            if (!option.includes(location)) {
                return false;
            }
        }
        return true;
    }
}
