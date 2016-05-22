package com.tngtech.archunit.junit;

public class RuleEvaluationException extends RuntimeException {
    public RuleEvaluationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public RuleEvaluationException(String format, Object... args) {
        super(String.format(format, args));
    }
}
