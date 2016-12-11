package com.tngtech.archunit.core;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class JavaStaticInitializer extends JavaCodeUnit<Method, MemberDescription.ForMethod> {
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    private JavaStaticInitializer(JavaClass clazz) {
        super(new StaticInitializerDescription(), clazz);
    }

    @Override
    public List<TypeDetails> getParameters() {
        return memberDescription.getParameterTypes();
    }

    @Override
    public TypeDetails getReturnType() {
        return memberDescription.getReturnType();
    }

    @Override
    public Set<? extends JavaAccess<?>> getAccessesToSelf() {
        return emptySet();
    }

    @Override
    public String toString() {
        return String.format("%s{owner=%s, name=%s}", getClass().getSimpleName(), getOwner(), getName());
    }

    static class Builder implements BuilderWithBuildParameter<JavaClass, JavaStaticInitializer> {
        @Override
        public JavaStaticInitializer build(JavaClass owner) {
            return new JavaStaticInitializer(owner);
        }
    }

    private static class StaticInitializerDescription implements MemberDescription.ForMethod {

        @Override
        public List<TypeDetails> getParameterTypes() {
            return emptyList();
        }

        @Override
        public TypeDetails getReturnType() {
            return TypeDetails.of(void.class);
        }

        @Override
        public String getName() {
            return STATIC_INITIALIZER_NAME;
        }

        @Override
        public int getModifiers() {
            return 0;
        }

        @Override
        public Set<JavaAnnotation> getAnnotations() {
            return emptySet();
        }

        @Override
        public String getDescriptor() {
            return "()V";
        }

        @Override
        public Method reflect() {
            throw new RuntimeException("Can't reflect a static initializer");
        }

        @Override
        public void checkCompatibility(JavaClass owner) {
        }
    }
}
