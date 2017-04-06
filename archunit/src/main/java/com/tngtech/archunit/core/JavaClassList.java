package com.tngtech.archunit.core;

import java.util.List;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.Function;

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

    public static final Function<JavaClassList, List<String>> GET_NAMES = new Function<JavaClassList, List<String>>() {
        @Override
        public List<String> apply(JavaClassList input) {
            return input.getNames();
        }
    };
}