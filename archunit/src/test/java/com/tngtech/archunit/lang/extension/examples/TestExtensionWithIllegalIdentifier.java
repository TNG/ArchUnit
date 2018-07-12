package com.tngtech.archunit.lang.extension.examples;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.extension.ArchUnitExtension;
import com.tngtech.archunit.lang.extension.EvaluatedRule;

import java.util.Properties;

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

    @Override
    public void onFinishAnalyzingClasses(JavaClasses classes) {
    }
}
