package com.tngtech.archunit.core;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImportOptions {
    private final Set<ImportOption> options;

    public ImportOptions() {
        this(Collections.<ImportOption>emptySet());
    }

    private ImportOptions(Set<ImportOption> options) {
        this.options = checkNotNull(options);
    }

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
