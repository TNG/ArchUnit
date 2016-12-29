package com.tngtech.archunit.core;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;

interface BuilderWithBuildParameter<PARAMETER, VALUE> {
    VALUE build(PARAMETER parameter, ImportedClasses.ByTypeName importedClasses);

    class BuildFinisher {
        static <PARAMETER, VALUE> Set<VALUE> build(
                Set<? extends BuilderWithBuildParameter<PARAMETER, ? extends VALUE>> builders,
                PARAMETER parameter,
                ImportedClasses.ByTypeName importedClasses) {
            checkNotNull(builders);
            checkNotNull(parameter);

            ImmutableSet.Builder<VALUE> result = ImmutableSet.builder();
            for (BuilderWithBuildParameter<PARAMETER, ? extends VALUE> builder : builders) {
                result.add(builder.build(parameter, importedClasses));
            }
            return result.build();
        }
    }
}
