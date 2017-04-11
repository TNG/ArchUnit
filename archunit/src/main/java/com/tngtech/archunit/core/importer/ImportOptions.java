package com.tngtech.archunit.core.importer;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class ImportOptions {
    private final Set<ImportOption> options;

    @PublicAPI(usage = ACCESS)
    public ImportOptions() {
        this(Collections.<ImportOption>emptySet());
    }

    private ImportOptions(Set<ImportOption> options) {
        this.options = checkNotNull(options);
    }

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
