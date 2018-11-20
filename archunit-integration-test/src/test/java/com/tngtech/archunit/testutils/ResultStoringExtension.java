package com.tngtech.archunit.testutils;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.extension.ArchUnitExtension;
import com.tngtech.archunit.lang.extension.EvaluatedRule;

import static com.google.common.base.Preconditions.checkNotNull;

public class ResultStoringExtension implements ArchUnitExtension {
    private static final String UNIQUE_IDENTIFIER = "archunit-result-storing-extension";

    private static final ConcurrentHashMap<String, EvaluationResult> storedResults = new ConcurrentHashMap<>();

    @Override
    public String getUniqueIdentifier() {
        return UNIQUE_IDENTIFIER;
    }

    @Override
    public void configure(Properties properties) {
    }

    @Override
    public void handle(EvaluatedRule evaluatedRule) {
        storedResults.put(evaluatedRule.getResult().getFailureReport().toString(), evaluatedRule.getResult());
    }

    public static void reset() {
        storedResults.clear();
    }

    static EvaluationResult getEvaluationResultFor(String errorMessage) {
        return checkNotNull(storedResults.get(errorMessage), "No result was recorded for error message: %s", errorMessage);
    }

    public static void enable() {
        ArchConfiguration.get().configureExtension(UNIQUE_IDENTIFIER).setProperty("enabled", true);
    }

    public static void disable() {
        ArchConfiguration.get().configureExtension(UNIQUE_IDENTIFIER).setProperty("enabled", false);
    }
}
