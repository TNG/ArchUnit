package com.tngtech.archunit.testutil.syntax;

import static com.google.common.base.MoreObjects.toStringHelper;

class Parameter {
    private final Object value;
    private final String description;

    Parameter(Object value, String description) {
        this.value = value;
        this.description = description;
    }

    public Object getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("value", getValue())
                .add("description", getDescription())
                .toString();
    }
}
