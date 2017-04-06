package com.tngtech.archunit.visual;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class VisualizationContext {
    private final Set<String> basePkgs;
    private final boolean includeEverything;
    private final boolean ignoreAccessToSuperConstructorFromConstructor;

    private VisualizationContext(Set<String> basePkgs, boolean includeEverything, boolean ignoreAccessToSuperConstructorFromConstructor) {
        this.basePkgs = ImmutableSet.copyOf(basePkgs);
        this.includeEverything = includeEverything;
        this.ignoreAccessToSuperConstructorFromConstructor = ignoreAccessToSuperConstructorFromConstructor;
    }

    boolean isElementIncluded(String fullname) {
        if (includeEverything) {
            return true;
        }
        for (String s : basePkgs) {
            if (fullname.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    boolean isDependencyIncluded(JsonJavaElement origin, String targetOwner, boolean constructorCall) {
        return !targetOwner.equals(origin.fullname) &&
                isElementIncluded(targetOwner) &&
                (!constructorCall || isIncludedConstructorCall(origin, targetOwner));
    }

    private boolean isIncludedConstructorCall(JsonJavaElement origin, String targetOwner) {
        return !ignoreAccessToSuperConstructorFromConstructor || !(origin instanceof JsonJavaClazz)
                || !((JsonJavaClazz) origin).hasAsSuperClass(targetOwner);
    }

    public static class Builder {
        private Set<String> basePkgs;
        private boolean includeEverything = false;
        private boolean ignoreAccessToSuperConstructorFromConstructor = false;

        public Builder() {
        }

        public Builder includeOnly(String... basePkgs) {
            return includeOnly(ImmutableSet.copyOf(basePkgs));
        }

        public Builder includeOnly(Set<String> basePkgs) {
            this.basePkgs = basePkgs;
            return this;
        }

        public Builder includeEverything() {
            basePkgs = new HashSet<>();
            includeEverything = true;
            return this;
        }

        public Builder ignoreAccessToSuperConstructor() {
            ignoreAccessToSuperConstructorFromConstructor = true;
            return this;
        }

        public VisualizationContext build() {
            return new VisualizationContext(basePkgs, includeEverything, ignoreAccessToSuperConstructorFromConstructor);
        }
    }
}
