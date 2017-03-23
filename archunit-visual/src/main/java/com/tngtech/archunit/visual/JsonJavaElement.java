package com.tngtech.archunit.visual;

import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.Set;

abstract class JsonJavaElement extends JsonElement {
    @Expose
    private Set<String> interfaces = new HashSet<>();
    @Expose
    private Set<JsonFieldAccess> fieldAccesses = new HashSet<>();
    @Expose
    private Set<JsonMethodCall> methodCalls = new HashSet<>();
    @Expose
    private Set<JsonConstructorCall> constructorCalls = new HashSet<>();
    @Expose
    private Set<String> anonImpl = new HashSet<>();
    @Expose
    protected Set<JsonJavaElement> children = new HashSet<>();

    JsonJavaElement(String name, String fullname, String type) {
        super(name, fullname, type);
    }

    @Override
    void insertJavaElement(JsonJavaElement el) {
        insertInnerClass(el);
    }

    @Override
    Set<? extends JsonElement> getChildren() {
        return children;
    }

    void insertInnerClass(JsonJavaElement el) {
        this.children.add(el);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JsonJavaElement that = (JsonJavaElement) o;

        if (interfaces != null ? !interfaces.equals(that.interfaces) : that.interfaces != null) return false;
        if (fieldAccesses != null ? !fieldAccesses.equals(that.fieldAccesses) : that.fieldAccesses != null)
            return false;
        if (methodCalls != null ? !methodCalls.equals(that.methodCalls) : that.methodCalls != null) return false;
        if (constructorCalls != null ? !constructorCalls.equals(that.constructorCalls) : that.constructorCalls != null)
            return false;
        return children != null ? children.equals(that.children) : that.children == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (interfaces != null ? interfaces.hashCode() : 0);
        result = 31 * result + (fieldAccesses != null ? fieldAccesses.hashCode() : 0);
        result = 31 * result + (methodCalls != null ? methodCalls.hashCode() : 0);
        result = 31 * result + (constructorCalls != null ? constructorCalls.hashCode() : 0);
        result = 31 * result + (children != null ? children.hashCode() : 0);
        return result;
    }
}
