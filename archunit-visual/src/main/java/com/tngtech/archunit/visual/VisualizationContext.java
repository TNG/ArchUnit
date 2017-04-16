package com.tngtech.archunit.visual;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class VisualizationContext {
    private final Set<String> rootPackages;
    private final boolean includeEverything;
    private final boolean ignoreAccessToSuperConstructorFromConstructor;

    private VisualizationContext(Set<String> rootPackages, boolean includeEverything, boolean ignoreAccessToSuperConstructorFromConstructor) {
        this.rootPackages = ImmutableSet.copyOf(rootPackages);
        this.includeEverything = includeEverything;
        this.ignoreAccessToSuperConstructorFromConstructor = ignoreAccessToSuperConstructorFromConstructor;
    }

    boolean isElementIncluded(String fullname) {
        if (includeEverything) {
            return true;
        }
        for (String s : rootPackages) {
            if (fullname.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    boolean isDependencyIncluded(JsonJavaElement origin, String targetOwner, boolean constructorCall) {
        // FIXME: targetOwner.equals(origin.fullname) has nothing to do with VisualizationContext
        // FIXME: constructorCall is redundant, at least it should be, i.e. target.isConstructor()
        return !targetOwner.equals(origin.fullname) &&
                isElementIncluded(targetOwner) &&
                (!constructorCall || isIncludedConstructorCall(origin, targetOwner));
    }

    private boolean isIncludedConstructorCall(JsonJavaElement origin, String targetOwner) {
        return !ignoreAccessToSuperConstructorFromConstructor || !(origin instanceof JsonJavaClass)
                || !((JsonJavaClass) origin).hasAsSuperClass(targetOwner);
    }

    public static class Builder {
        private Set<String> rootPackages;
        private boolean includeEverything = false;
        private boolean ignoreAccessToSuperConstructorFromConstructor = false;

        public Builder() {
        }

        public Builder includeOnly(String... rootPackages) {
            return includeOnly(ImmutableSet.copyOf(rootPackages));
        }

        public Builder includeOnly(Set<String> rootPackages) {
            this.rootPackages = rootPackages;
            return this;
        }

        public Builder includeEverything() {
            rootPackages = new HashSet<>();
            includeEverything = true;
            return this;
        }

        public Builder ignoreAccessToSuperConstructor() {
            ignoreAccessToSuperConstructorFromConstructor = true;
            return this;
        }

        public VisualizationContext build() {
            return new VisualizationContext(rootPackages, includeEverything, ignoreAccessToSuperConstructorFromConstructor);
        }
    }
}
