package com.tngtech.archunit.visual;

import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

public class VisualizationContext {
    private Set<String> basePkgs;

    private VisualizationContext(Set<String> basePkgs) {
        this.basePkgs = basePkgs;
    }

    public static VisualizationContext includeEverything() {
        return new VisualizationContext(new HashSet<String>());
    }

    public static VisualizationContext includeOnly(Set<String> basePkgs) {
        return new VisualizationContext(basePkgs);
    }

    public static VisualizationContext includeOnly(String... basePkgs) {
        return includeOnly(ImmutableSet.copyOf(basePkgs));
    }

    public boolean isRelevant(String fullname) {
        for (String s : basePkgs) {
            if (fullname.startsWith(s)) {
                return true;
            }
        }
        return false;
    }
}
