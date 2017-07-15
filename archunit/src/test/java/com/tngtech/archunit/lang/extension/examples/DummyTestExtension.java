package com.tngtech.archunit.lang.extension.examples;

import java.util.Properties;

import com.tngtech.archunit.lang.extension.ArchUnitExtension;
import com.tngtech.archunit.lang.extension.EvaluatedRule;

public class DummyTestExtension implements ArchUnitExtension {
    public static final String UNIQUE_IDENTIFIER = DummyTestExtension.class.getName().replace(".", "_");

    @Override
    public String getUniqueIdentifier() {
        return UNIQUE_IDENTIFIER;
    }

    @Override
    public void configure(Properties properties) {
    }

    @Override
    public void handle(EvaluatedRule evaluatedRule) {
    }
}
