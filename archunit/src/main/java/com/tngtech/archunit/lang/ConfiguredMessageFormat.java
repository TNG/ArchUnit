package com.tngtech.archunit.lang;

import com.google.common.base.Joiner;

class ConfiguredMessageFormat {
    private static final ConfiguredMessageFormat instance = new ConfiguredMessageFormat();

    public static ConfiguredMessageFormat get() {
        return instance;
    }

    public String format(ClosedArchRule<?> rule, FailureMessages failureMessages, Priority priority) {
        String violationTexts = Joiner.on(System.lineSeparator()).join(failureMessages);
        String priorityPrefix = String.format("Architecture Violation [Priority: %s] - ", priority.asString());
        String message = String.format("Rule '%s' was violated:%n%s", rule, violationTexts);
        return priorityPrefix + message;
    }
}
