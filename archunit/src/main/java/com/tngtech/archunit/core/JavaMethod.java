package com.tngtech.archunit.core;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaMethod extends JavaCodeUnit<Method, MemberDescription.ForMethod> {
    private Supplier<Set<JavaMethodCall>> callsToSelf = Suppliers.ofInstance(Collections.<JavaMethodCall>emptySet());

    private JavaMethod(Builder builder) {
        super(builder);
    }

    public Set<JavaMethodCall> getCallsOfSelf() {
        return getAccessesToSelf();
    }

    @Override
    public Set<JavaMethodCall> getAccessesToSelf() {
        return callsToSelf.get();
    }

    void registerCallsToMethod(Supplier<Set<JavaMethodCall>> calls) {
        this.callsToSelf = checkNotNull(calls);
    }

    static class Builder extends JavaCodeUnit.Builder<JavaMethod, Builder> {
        @Override
        JavaMethod construct(Builder builder) {
            return new JavaMethod(builder);
        }
    }
}
