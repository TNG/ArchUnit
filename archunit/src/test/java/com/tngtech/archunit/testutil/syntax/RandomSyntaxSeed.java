package com.tngtech.archunit.testutil.syntax;

import com.google.common.reflect.TypeToken;

public class RandomSyntaxSeed<T> {
    private final TypeToken<T> type;
    private final T value;
    private final String description;

    public RandomSyntaxSeed(Class<T> type, T value, String description) {
        this.type = TypeToken.of(type);
        this.value = value;
        this.description = description;
    }

    public TypeToken<T> getType() {
        return type;
    }

    public T getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
