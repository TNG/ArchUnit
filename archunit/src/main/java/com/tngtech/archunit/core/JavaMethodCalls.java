package com.tngtech.archunit.core;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ForwardingSet;

public class JavaMethodCalls extends ForwardingSet<JavaMethodCall> {
    private final Set<JavaMethodCall> methodCalls = new HashSet<>();

    @Override
    protected Set<JavaMethodCall> delegate() {
        return methodCalls;
    }
}
