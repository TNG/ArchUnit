package com.tngtech.archunit.core;

import java.util.Collections;
import java.util.Set;

import org.objectweb.asm.Type;

import static java.util.Collections.emptySet;

public class JavaStaticInitializer extends JavaCodeUnit {
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    private JavaStaticInitializer(Builder builder) {
        super(builder);
    }

    @Override
    public Set<? extends JavaAccess<?>> getAccessesToSelf() {
        return emptySet();
    }

    @Override
    public String toString() {
        return String.format("%s{owner=%s, name=%s}", getClass().getSimpleName(), getOwner(), getName());
    }

    static class Builder extends JavaCodeUnit.Builder<JavaStaticInitializer, Builder> {
        public Builder() {
            withReturnType(Type.getType(void.class));
            withParameters(new Type[0]);
            withName(STATIC_INITIALIZER_NAME);
            withDescriptor("()V");
            withAnnotations(Collections.<JavaAnnotation.Builder>emptySet());
            withModifiers(Collections.<JavaModifier>emptySet());
        }

        @Override
        JavaStaticInitializer construct(Builder builder) {
            return new JavaStaticInitializer(builder);
        }
    }
}
