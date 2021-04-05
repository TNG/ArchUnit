package com.tngtech.archunit.library.metrics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.tngtech.archunit.base.Function;

class TestElement {
    private final String name;
    private final Set<TestElement> dependencies = new HashSet<>();

    TestElement() {
        this("irrelevant");
    }

    TestElement(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }

    void addDependency(TestElement element) {
        dependencies.add(element);
    }

    static final Function<TestElement, Collection<TestElement>> GET_DEPENDENCIES = new Function<TestElement, Collection<TestElement>>() {
        @Override
        public Collection<TestElement> apply(TestElement element) {
            return element.dependencies;
        }
    };
}
