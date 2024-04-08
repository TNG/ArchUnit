package com.tngtech.archunit.example.layers;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@SuppressWarnings({"unused", "MultipleInjectedConstructorsForClass"})
public class ClassViolatingInjectionRules {
    private String okayBecauseNotInjected;
    @Autowired
    private Object badBecauseAutowiredField;
    @Value("${name}")
    private List<?> badBecauseValueField;
    @javax.inject.Inject
    private Set<?> badBecauseJavaxInjectField;
    @com.google.inject.Inject
    private Map<?, ?> badBecauseComGoogleInjectField;
    @javax.annotation.Resource
    private File badBecauseJavaxResourceField;
    @jakarta.inject.Inject
    private Object badBecauseJakartaInjectField;
    @jakarta.annotation.Resource
    private Object badBecauseJakartaResourceField;

    ClassViolatingInjectionRules(String okayBecauseNotInjected) {
    }

    @Autowired
    ClassViolatingInjectionRules(Object okayBecauseAutowiredConstructor) {
    }

    ClassViolatingInjectionRules(@Value("${name}") List<?> okayBecauseValueConstructorParameter) {
    }

    @javax.inject.Inject
    ClassViolatingInjectionRules(Set<?> okayBecauseJavaxInjectConstructor) {
    }

    @com.google.inject.Inject
    ClassViolatingInjectionRules(Map<?, ?> okayBecauseComGoogleInjectConstructor) {
    }

    void someMethod(String okayBecauseNotInjected) {
    }

    @Autowired
    void someMethod(Object okayBecauseAutowiredMethod) {
    }

    void someMethod(@Value("${name}") List<?> okayBecauseValueMethodParameter) {
    }

    @javax.inject.Inject
    void someMethod(Set<?> okayBecauseJavaxInjectMethod) {
    }

    @com.google.inject.Inject
    void someMethod(Map<?, ?> okayBecauseComGoogleInjectMethod) {
    }

    @javax.annotation.Resource
    void someMethod(File okayBecauseJavaxResourceMethod) {
    }

    @jakarta.inject.Inject
    void someMethod(Void okayBecauseJakartaInjectMethod) {
    }

    @jakarta.annotation.Resource
    void someMethod(Integer okayBecauseJavaxResourceMethod) {
    }
}
