package com.tngtech.archunit.core.domain;

import java.util.List;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Function;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class JavaClassList extends ForwardingList<JavaClass> {
    private final ImmutableList<JavaClass> elements;

    JavaClassList(List<JavaClass> elements) {
        this.elements = ImmutableList.copyOf(elements);
    }

    @Override
    protected List<JavaClass> delegate() {
        return elements;
    }

    @PublicAPI(usage = ACCESS)
    public List<String> getNames() {
        ImmutableList.Builder<String> result = ImmutableList.builder();
        for (JavaClass parameter : this) {
            result.add(parameter.getName());
        }
        return result.build();
    }

    @PublicAPI(usage = ACCESS)
    public static final Function<JavaClassList, List<String>> GET_NAMES = new Function<JavaClassList, List<String>>() {
        @Override
        public List<String> apply(JavaClassList input) {
            return input.getNames();
        }
    };
}