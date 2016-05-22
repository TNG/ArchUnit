package com.tngtech.archunit.core;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ForwardingSet;

public class JavaConstructorCalls extends ForwardingSet<JavaConstructorCall> {
    private final Set<JavaConstructorCall> constructorCalls = new HashSet<>();

    @Override
    protected Set<JavaConstructorCall> delegate() {
        return constructorCalls;
    }
}
