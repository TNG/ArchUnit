package com.tngtech.archunit.lang;

public class ArchAssertionError extends AssertionError {
    private final Priority priority;

    public ArchAssertionError(Priority priority, String message) {
        super(message);
        this.priority = priority;
    }

    public Priority getPriority() {
        return priority;
    }
}
