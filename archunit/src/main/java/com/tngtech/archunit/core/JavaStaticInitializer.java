package com.tngtech.archunit.core;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.Set;

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
    public Member reflect() {
        throw new UnsupportedOperationException("Can't reflect on a static initializer");
    }

    static class Builder extends JavaCodeUnit.Builder<JavaStaticInitializer, Builder> {
        public Builder() {
            withReturnType(JavaType.From.name(void.class.getName()));
            withParameters(Collections.<JavaType>emptyList());
            withName(STATIC_INITIALIZER_NAME);
            withDescriptor("()V");
            withAnnotations(Collections.<JavaAnnotation.Builder>emptySet());
            withModifiers(Collections.<JavaModifier>emptySet());
        }

        @Override
        JavaStaticInitializer construct(Builder builder, ImportedClasses.ByTypeName importedClasses) {
            return new JavaStaticInitializer(builder);
        }
    }
}
