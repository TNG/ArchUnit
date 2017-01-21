package com.tngtech.archunit.lang;

import java.util.Collection;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.properties.HasDescription;

class ConfiguredMessageFormat {
    private static final ConfiguredMessageFormat instance = new ConfiguredMessageFormat();

    static ConfiguredMessageFormat get() {
        return instance;
    }

    String formatFailure(HasDescription rule, Collection<String> failureMessages, Priority priority) {
        String violationTexts = Joiner.on(System.lineSeparator()).join(failureMessages);
        String priorityPrefix = String.format("Architecture Violation [Priority: %s] - ", priority.asString());
        String message = String.format("Rule '%s' was violated:%n%s", rule.getDescription(), violationTexts);
        return priorityPrefix + message;
    }

    <T> String formatRuleText(HasDescription itemsUnderTest, ArchCondition<T> condition) {
        return String.format("%s should %s", itemsUnderTest.getDescription(), condition.getDescription());
    }
}
