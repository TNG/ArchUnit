package com.tngtech.archunit.core;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class JavaMethod extends JavaCodeUnit<Method, MemberDescription.ForMethod> {
    private Set<JavaMethodCall> calls = Collections.emptySet();

    private JavaMethod(Builder builder) {
        super(builder);
    }

    @Override
    public List<TypeDetails> getParameters() {
        return memberDescription.getParameterTypes();
    }

    @Override
    public TypeDetails getReturnType() {
        return memberDescription.getReturnType();
    }

    public Set<JavaMethodCall> getCallsOfSelf() {
        return getAccessesToSelf();
    }

    @Override
    public Set<JavaMethodCall> getAccessesToSelf() {
        return calls;
    }

    public void registerCalls(Collection<JavaMethodCall> calls) {
        this.calls = ImmutableSet.copyOf(calls);
    }

    static class Builder extends JavaMember.Builder<MemberDescription.ForMethod, JavaMethod> {
        @Override
        public JavaMethod build(JavaClass owner) {
            this.owner = owner;
            return new JavaMethod(this);
        }

        public BuilderWithBuildParameter<JavaClass, JavaMethod> withMethod(Method method) {
            return withMember(new MemberDescription.ForDeterminedMethod(method));
        }
    }
}
