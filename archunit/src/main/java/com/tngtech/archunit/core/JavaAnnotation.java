package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(annotation, owner);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaAnnotation other = (JavaAnnotation) obj;
        return Objects.equals(this.annotation, other.annotation)
                && Objects.equals(this.owner, other.owner);
    }

    static class Builder implements BuilderWithBuildParameter<JavaMember<?, ?>, JavaAnnotation<?>> {
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
