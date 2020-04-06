package com.tngtech.archunit.example.layers;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

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
    @Resource
    private File badBecauseResourceField;

    ClassViolatingInjectionRules(String okayBecauseNotInjected) {
    }

    @Autowired
    ClassViolatingInjectionRules(Object badBecauseAutowiredField) {
    }

    ClassViolatingInjectionRules(@Value("${name}") List<?> badBecauseValueField) {
    }

    @javax.inject.Inject
    ClassViolatingInjectionRules(Set<?> badBecauseJavaxInjectField) {
    }

    @com.google.inject.Inject
    ClassViolatingInjectionRules(Map<?, ?> badBecauseComGoogleInjectField) {
    }

    void someMethod(String okayBecauseNotInjected) {
    }

    @Autowired
    void someMethod(Object badBecauseAutowiredField) {
    }

    void someMethod(@Value("${name}") List<?> badBecauseValueField) {
    }

    @javax.inject.Inject
    void someMethod(Set<?> badBecauseJavaxInjectField) {
    }

    @com.google.inject.Inject
    void someMethod(Map<?, ?> badBecauseComGoogleInjectField) {
    }

    @Resource
    void someMethod(File badBecauseResourceField) {
    }
}
