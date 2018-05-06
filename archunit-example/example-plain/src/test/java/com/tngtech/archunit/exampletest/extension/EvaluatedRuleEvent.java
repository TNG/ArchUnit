package com.tngtech.archunit.exampletest.extension;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.extension.EvaluatedRule;

public class EvaluatedRuleEvent {
    private final EvaluatedRule evaluatedRule;

    EvaluatedRuleEvent(EvaluatedRule evaluatedRule) {
        this.evaluatedRule = evaluatedRule;
    }

    public boolean contains(ArchRule rule) {
        return evaluatedRule.getRule().getDescription().equals(rule.getDescription());
    }

    public boolean contains(JavaClasses classes) {
        return evaluatedRule.getClasses().equals(classes);
    }

    public boolean hasViolationFor(Class<?> clazz) {
        return evaluatedRule.getResult().getFailureReport().toString().contains(clazz.getSimpleName());
    }
}
