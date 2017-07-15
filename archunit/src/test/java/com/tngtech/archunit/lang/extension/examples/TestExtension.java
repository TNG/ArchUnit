package com.tngtech.archunit.lang.extension.examples;

import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.lang.extension.ArchUnitExtension;
import com.tngtech.archunit.lang.extension.EvaluatedRule;

public class TestExtension implements ArchUnitExtension {
    public static final String UNIQUE_IDENTIFIER = "test-extension";

    private final String identifier;

    private ImmutableMap<Object, Object> configuredProperties;
    private EvaluatedRule evaluatedRule;

    public TestExtension() {
        this(UNIQUE_IDENTIFIER);
    }

    public TestExtension(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getUniqueIdentifier() {
        return identifier;
    }

    @Override
    public void configure(Properties properties) {
        configuredProperties = ImmutableMap.copyOf(properties);
    }

    @Override
    public void handle(EvaluatedRule evaluatedRule) {
        this.evaluatedRule = evaluatedRule;
    }

    public ImmutableMap<Object, Object> getConfiguredProperties() {
        return configuredProperties;
    }

    public EvaluatedRule getEvaluatedRule() {
        return evaluatedRule;
    }

    public boolean wasNeverCalled() {
        return configuredProperties == null && evaluatedRule == null;
    }
}
