package com.tngtech.archunit.core;

import java.lang.reflect.Method;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class JavaMethod extends JavaMethodLike<Method, MemberDescription.ForMethod> {
    private JavaMethod(Builder builder) {
        super(builder);
    }

    @Override
    public List<Class<?>> getParameters() {
        return ImmutableList.copyOf(memberDescription.getParameterTypes());
    }

    @Override
    public Class<?> getReturnType() {
        return memberDescription.getReturnType();
    }

    @Override
    public String getDescriptor() {
        return memberDescription.getDescriptor();
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
