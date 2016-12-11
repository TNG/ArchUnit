package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class JavaConstructor extends JavaCodeUnit {
    private Set<JavaConstructorCall> callsToSelf = Collections.emptySet();

    public static final String CONSTRUCTOR_NAME = "<init>";

    private JavaConstructor(Builder builder) {
        super(builder);
    }

    @Override
    public boolean isConstructor() {
        return true;
    }

    public Set<JavaConstructorCall> getCallsOfSelf() {
        return getAccessesToSelf();
    }

    @Override
    public Set<JavaConstructorCall> getAccessesToSelf() {
        return callsToSelf;
    }

    void registerCallsToConstructor(Collection<JavaConstructorCall> calls) {
        this.callsToSelf = ImmutableSet.copyOf(calls);
    }

    static final class Builder extends JavaCodeUnit.Builder<JavaConstructor, Builder> {
        @Override
        JavaConstructor construct(Builder builder) {
            return new JavaConstructor(builder);
        }
    }
}
