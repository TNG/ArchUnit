package com.tngtech.archunit.visual;

import java.util.HashSet;
import java.util.Set;

class JsonJavaFile extends JsonJavaElement {
    private Set<String> interfaces = new HashSet<>();
    private Set<JsonFieldAccess> fieldAccesses = new HashSet<>();
    private Set<JsonMethodCall> methodCalls = new HashSet<>();
    private Set<JsonConstructorCall> constructorCalls = new HashSet<>();

    JsonJavaFile(String name, String fullname, String type) {
        super(name, fullname, type);
    }

    void addInterface(String i) {
        interfaces.add(i);
    }

    void addFieldAccess(JsonFieldAccess f) {
        fieldAccesses.add(f);
    }

    void addMethodCall(JsonMethodCall m) {
        methodCalls.add(m);
    }

    void addConstructorCall(JsonConstructorCall c) {
        constructorCalls.add(c);
    }
}
