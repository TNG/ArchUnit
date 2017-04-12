package com.tngtech.archunit.testutil.syntax;

public class RandomSyntaxSeed<T> {
    private final Class<T> type;
    private final T value;
    private final String description;

    public RandomSyntaxSeed(Class<T> type, T value, String description) {
        this.type = type;
        this.value = value;
        this.description = description;
    }

    public Class<T> getType() {
        return type;
    }

    public T getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
