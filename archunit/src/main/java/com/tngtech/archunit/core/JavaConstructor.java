package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class JavaConstructor extends JavaCodeUnit<Constructor<?>, MemberDescription.ForConstructor> {
    private Set<JavaConstructorCall> calls = Collections.emptySet();

    public static final String CONSTRUCTOR_NAME = "<init>";

    private JavaConstructor(Builder builder) {
        super(builder);
    }

    @Override
    public List<TypeDetails> getParameters() {
        return Lists.newArrayList(memberDescription.getParameterTypes());
    }

    @Override
    public TypeDetails getReturnType() {
        return TypeDetails.of(void.class);
    }

    @Override
    public boolean isConstructor() {
        return true;
    }

    public Set<JavaConstructorCall> getCallsOfSelf() {
        return getAccessesToSelf();
    }

    @Override
    public Set<JavaConstructorCall> getAccessesToSelf() {
        return calls;
    }

    public void registerCalls(Collection<JavaConstructorCall> calls) {
        this.calls = ImmutableSet.copyOf(calls);
    }

    static final class Builder extends JavaMember.Builder<MemberDescription.ForConstructor, JavaConstructor> {
        @Override
        public JavaConstructor build(JavaClass owner) {
            this.owner = owner;
            return new JavaConstructor(this);
        }

        public BuilderWithBuildParameter<JavaClass, JavaConstructor> withConstructor(Constructor<?> constructor) {
            return withMember(new MemberDescription.ForConstructor(constructor));
        }
    }
}
