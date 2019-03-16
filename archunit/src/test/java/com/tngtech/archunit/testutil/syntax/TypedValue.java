package com.tngtech.archunit.testutil.syntax;

import java.lang.reflect.Type;

import com.google.common.base.MoreObjects;

class TypedValue {
    private final PropagatedType type;
    private final Object value;

    // NOTE: type != value.getClass(), i.e. it's important what exactly the interface method returned
    TypedValue(PropagatedType type, Object value) {
        this.type = type;
        this.value = value;
    }

    PropagatedType getType() {
        return type;
    }

    Object getValue() {
        return value;
    }

    Class<?> getRawType() {
        return getType().getRawType();
    }

    PropagatedType resolveType(Type type) {
        return this.type.resolveType(type);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", getType())
                .add("value", getValue())
                .toString();
    }
}
