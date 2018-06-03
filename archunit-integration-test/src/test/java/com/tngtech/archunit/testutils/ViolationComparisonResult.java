package com.tngtech.archunit.testutils;

class ViolationComparisonResult {
    private final String message;

    private ViolationComparisonResult(String message) {
        this.message = message;
    }

    static ViolationComparisonResult success() {
        return new ViolationComparisonResult("");
    }

    static ViolationComparisonResult failure(String message) {
        return new ViolationComparisonResult(message);
    }

    boolean isSuccess() {
        return message.isEmpty();
    }

    String describe() {
        return message;
    }
}
