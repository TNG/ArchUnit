package com.tngtech.archunit.core;

import java.util.List;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;

public class JavaClassList extends ForwardingList<JavaClass> {
    private final ImmutableList<JavaClass> elements;

    JavaClassList(List<JavaClass> elements) {
        this.elements = ImmutableList.copyOf(elements);
    }

    @Override
    protected List<JavaClass> delegate() {
        return elements;
    }

    public List<String> getNames() {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (JavaClass parameter : this) {
            result.add(parameter.getName());
        }
        return result.build();
    }
}