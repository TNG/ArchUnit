package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.util.List;

import com.google.common.collect.Lists;

public class JavaConstructor extends JavaCodeUnit<Constructor<?>, MemberDescription.ForConstructor> {
    public static final String CONSTRUCTOR_NAME = "<init>";

    private JavaConstructor(Builder builder) {
        super(builder);
    }

    @Override
    public List<Class<?>> getParameters() {
        return Lists.newArrayList(memberDescription.getParameterTypes());
    }

    @Override
    public Class<?> getReturnType() {
        return void.class;
    }

    @Override
    public boolean isConstructor() {
        return true;
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
