package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;

public class JavaAnnotation<T extends Annotation> implements HasOwner<JavaMember<?, ?>> {
    private final T annotation;
    private final JavaMember<?, ?> owner;

    @SuppressWarnings("unchecked")
    private JavaAnnotation(Builder builder) {
        annotation = (T) builder.annotation;
        owner = builder.owner;
    }

    public Class<?> getType() {
        return annotation.annotationType();
    }

    @Override
    public JavaMember<?, ?> getOwner() {
        return owner;
    }

    public T reflect() {
        return annotation;
    }

    static final class Builder implements BuilderWithBuildParameter<JavaMember<?, ?>, JavaAnnotation<?>> {
        private Annotation annotation;
        private JavaMember<?, ?> owner;

        Builder withAnnotation(Annotation annotation) {
            this.annotation = annotation;
            return this;
        }

        @Override
        public JavaAnnotation<?> build(JavaMember<?, ?> owner) {
            this.owner = owner;
            return new JavaAnnotation(this);
        }
    }
}
