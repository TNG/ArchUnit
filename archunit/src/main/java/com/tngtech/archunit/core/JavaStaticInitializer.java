package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class JavaStaticInitializer extends JavaCodeUnit<Method, MemberDescription.ForMethod> {
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    private int hashCode;

    private JavaStaticInitializer(JavaClass clazz) {
        super(new StaticInitializerDescription(), clazz);
        hashCode = Objects.hash(getFullName());
    }

    @Override
    public List<Class<?>> getParameters() {
        return memberDescription.getParameterTypes();
    }

    @Override
    public Class<?> getReturnType() {
        return memberDescription.getReturnType();
    }

    @Override
    public Set<? extends JavaAccess<?>> getAccessesToSelf() {
        return emptySet();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaStaticInitializer other = (JavaStaticInitializer) obj;
        return Objects.equals(getFullName(), other.getFullName());
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
        public List<Class<?>> getParameterTypes() {
            return emptyList();
        }

        @Override
        public Class<?> getReturnType() {
            return void.class;
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
        public Annotation[] getAnnotations() {
            return new Annotation[0];
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
