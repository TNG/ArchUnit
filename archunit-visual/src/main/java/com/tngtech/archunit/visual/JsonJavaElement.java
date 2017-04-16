package com.tngtech.archunit.visual;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.annotations.Expose;

abstract class JsonJavaElement extends JsonElement {
    @Expose
    private Set<String> interfaces = new HashSet<>();
    @Expose
    private Set<JsonFieldAccess> fieldAccesses = new HashSet<>();
    @Expose
    private Set<JsonMethodCall> methodCalls = new HashSet<>();
    @Expose
    private Set<JsonConstructorCall> constructorCalls = new HashSet<>();
    // FIXME: Don't use cryptic shortcuts like 'anonImpl', esp. within public API
    @Expose
    private Set<String> anonImpl = new HashSet<>();
    @Expose
    private Set<JsonJavaElement> children = new HashSet<>();

    JsonJavaElement(String name, String fullname, String type) {
        super(name, fullname, type);
    }

    @Override
    void insertJavaElement(JsonJavaElement el) {
        this.children.add(el);
    }

    @Override
    Set<? extends JsonElement> getChildren() {
        return children;
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

    void addAnonImpl(String i) {
        anonImpl.add(i);
    }
}
