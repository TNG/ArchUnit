/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptySet;

final class ImportOptions {
    private final Set<ImportOption> options;

    ImportOptions() {
        this(emptySet());
    }

    private ImportOptions(Set<ImportOption> options) {
        this.options = checkNotNull(options);
    }

    ImportOptions with(ImportOption option) {
        return new ImportOptions(ImmutableSet.<ImportOption>builder().addAll(options).add(option).build());
    }

    ImportOptions with(Collection<ImportOption> options) {
        return new ImportOptions(Sets.union(this.options, ImmutableSet.copyOf(options)));
    }

    boolean include(Location location) {
        return options.stream().allMatch(option -> option.includes(location));
    }
}
