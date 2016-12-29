package com.tngtech.archunit.junit;

class RuleEvaluationException extends RuntimeException {
    RuleEvaluationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    RuleEvaluationException(String format, Object... args) {
        super(String.format(format, args));
    }
}
