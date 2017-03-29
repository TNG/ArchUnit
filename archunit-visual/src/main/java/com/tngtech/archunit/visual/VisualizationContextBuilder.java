package com.tngtech.archunit.visual;

import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

public class VisualizationContextBuilder {
    private VisualizationContext context = new VisualizationContext();

    public VisualizationContextBuilder() {
    }

    public VisualizationContextBuilder includeOnly(String... basePkgs) {
        return includeOnly(ImmutableSet.copyOf(basePkgs));
    }

    public VisualizationContextBuilder includeOnly(Set<String> basePkgs) {
        context.setBasePkgs(basePkgs);
        return this;
    }

    public VisualizationContextBuilder includeEverything() {
        context.setBasePkgs(new HashSet<String>());
        context.setIncludeEverything();
        return this;
    }

    public VisualizationContextBuilder ignoreAccessToSuperConstructor() {
        context.setIgnoreAccessToSuperConstructorFromConstructor(true);
        return this;
    }

    public VisualizationContext build() {
        return context;
    }
}
