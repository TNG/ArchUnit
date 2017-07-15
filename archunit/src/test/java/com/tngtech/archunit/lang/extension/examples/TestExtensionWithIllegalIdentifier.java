package com.tngtech.archunit.lang.extension.examples;

import java.util.Properties;

import com.tngtech.archunit.lang.extension.ArchUnitExtension;
import com.tngtech.archunit.lang.extension.EvaluatedRule;

public class TestExtensionWithIllegalIdentifier implements ArchUnitExtension {
    @Override
    public String getUniqueIdentifier() {
        return "illegal_because.of.dot";
    }

    @Override
    public void configure(Properties properties) {
    }

    @Override
    public void handle(EvaluatedRule evaluatedRule) {
    }
}
