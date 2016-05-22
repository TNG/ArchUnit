package com.tngtech.archunit.core;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

interface BuilderWithBuildParameter<PARAMETER, VALUE> {
    VALUE build(PARAMETER clazz);

    class BuildFinisher {
        public static <PARAMETER, VALUE> Set<VALUE> build(Set<? extends BuilderWithBuildParameter<PARAMETER, ? extends VALUE>> builders, PARAMETER parameter) {
            checkNotNull(builders);
            checkNotNull(parameter);

            Set<VALUE> result = new HashSet<>();
            for (BuilderWithBuildParameter<PARAMETER, ? extends VALUE> builder : builders) {
                result.add(builder.build(parameter));
            }
            return result;
        }
    }
}
