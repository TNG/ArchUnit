package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class JavaAnnotation implements HasOwner<JavaMember<?, ?>> {
    private final Annotation annotation;
    private final JavaMember<?, ?> owner;

    @SuppressWarnings("unchecked")
    private JavaAnnotation(Builder builder) {
        annotation = builder.annotation;
        owner = builder.owner;
    }

    public Class<?> getType() {
        return annotation.annotationType();
    }

    @Override
    public JavaMember<?, ?> getOwner() {
        return owner;
    }

    public Object get(String annotationMethodName) {
        try {
            Object result = getType().getMethod(annotationMethodName).invoke(annotation);
            if (result instanceof Class) {
                return TypeDetails.of((Class<?>) result);
            }
            if (result instanceof Class[]) {
                return TypeDetails.allOf((Class<?>[]) result);
            }
            if (result instanceof Enum<?>) {
                return new JavaEnumConstant(TypeDetails.of(((Enum) result).getDeclaringClass()), ((Enum) result).name());
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getProperties() {
        ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        for (Method method : getType().getDeclaredMethods()) {
            result.put(method.getName(), get(method.getName()));
        }
        return result.build();
    }

    static class Builder implements BuilderWithBuildParameter<JavaMember<?, ?>, JavaAnnotation> {
        private Annotation annotation;
        private JavaMember<?, ?> owner;

        Builder withAnnotation(Annotation annotation) {
            this.annotation = annotation;
            return this;
        }

        @Override
        public JavaAnnotation build(JavaMember<?, ?> owner) {
            this.owner = owner;
            return new JavaAnnotation(this);
        }
    }
}
