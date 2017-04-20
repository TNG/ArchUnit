package com.tngtech.archunit.visual;

import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

public class VisualizationContext {
    private final Set<String> rootPackages;

    private VisualizationContext(Set<String> rootPackages) {
        this.rootPackages = ImmutableSet.copyOf(rootPackages);
    }

    //FIXME: Test fuer verschiedene Konfigurationen vom Context
    boolean isElementIncluded(String fullname) {
        if (rootPackages.isEmpty()) {
            return true;
        }
        for (String s : rootPackages) {
            if (fullname.equals(s) || (fullname.startsWith(s) && fullname.substring(s.length()).startsWith("."))) {
                return true;
            }
        }
        return false;
    }

    public static class Builder {
        private Set<String> rootPackages = new HashSet<>();

        public Builder() {
        }

        public Builder includeOnly(String... rootPackages) {
            return includeOnly(ImmutableSet.copyOf(rootPackages));
        }

        public Builder includeOnly(Set<String> rootPackages) {
            this.rootPackages = rootPackages;
            return this;
        }

        public VisualizationContext build() {
            return new VisualizationContext(rootPackages);
        }
    }
}
