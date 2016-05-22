package com.tngtech.archunit.core;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ForwardingSet;

public class JavaFieldAccesses extends ForwardingSet<JavaFieldAccess> {
    private final Set<JavaFieldAccess> fieldAccesses = new HashSet<>();

    @Override
    protected Set<JavaFieldAccess> delegate() {
        return fieldAccesses;
    }
}
