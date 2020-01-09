package com.tngtech.archunit.testutil.assertion;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.JavaAnnotation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.testutil.TestUtils.invoke;

public class JavaAnnotationAssertion {
    @SuppressWarnings("rawtypes")
    public static Set<Map<String, Object>> propertiesOf(Set<? extends JavaAnnotation<?>> annotations) {
        List<Annotation> converted = new ArrayList<>();
        for (JavaAnnotation<?> annotation : annotations) {
            converted.add(annotation.as((Class) annotation.getRawType().reflect()));
        }
        return propertiesOf(converted.toArray(new Annotation[0]));
    }

    public static Set<Map<String, Object>> propertiesOf(Annotation[] annotations) {
        Set<Map<String, Object>> result = new HashSet<>();
        for (Annotation annotation : annotations) {
            result.add(propertiesOf(annotation));
        }
        return result;
    }

    private static Map<String, Object> propertiesOf(Annotation annotation) {
        Map<String, Object> props = new HashMap<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            Object returnValue = invoke(method, annotation);
            props.put(method.getName(), valueOf(returnValue));
        }
        return props;
    }

    private static Object valueOf(Object value) {
        if (value.getClass().isArray() && value.getClass().getComponentType().isPrimitive()) {
            return listFrom(value);
        }
        if (value instanceof String[]) {
            return ImmutableList.copyOf((String[]) value);
        }
        if (value instanceof Class) {
            return new SimpleTypeReference(((Class<?>) value).getName());
        }
        if (value instanceof Class[]) {
            return SimpleTypeReference.allOf((Class<?>[]) value);
        }
        if (value instanceof Enum) {
            return new SimpleEnumConstantReference((Enum<?>) value);
        }
        if (value instanceof Enum[]) {
            return SimpleEnumConstantReference.allOf((Enum<?>[]) value);
        }
        if (value instanceof Annotation) {
            return propertiesOf((Annotation) value);
        }
        if (value instanceof Annotation[]) {
            return propertiesOf((Annotation[]) value);
        }
        return value;
    }

    private static List<?> listFrom(Object primitiveArray) {
        checkArgument(primitiveArray.getClass().getComponentType().equals(int.class), "Only supports int[] at the moment, please extend");
        ImmutableList.Builder<Integer> result = ImmutableList.builder();
        for (int anInt : (int[]) primitiveArray) {
            result.add(anInt);
        }
        return result.build();
    }

    private static class SimpleTypeReference {
        private final String typeName;

        private SimpleTypeReference(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final SimpleTypeReference other = (SimpleTypeReference) obj;
            return Objects.equals(this.typeName, other.typeName);
        }

        @Override
        public String toString() {
            return typeName;
        }

        static List<SimpleTypeReference> allOf(Class<?>[] value) {
            ImmutableList.Builder<SimpleTypeReference> result = ImmutableList.builder();
            for (Class<?> c : value) {
                result.add(new SimpleTypeReference(c.getName()));
            }
            return result.build();
        }
    }

    private static class SimpleEnumConstantReference {
        private final SimpleTypeReference type;
        private final String name;

        SimpleEnumConstantReference(Enum<?> value) {
            this.type = new SimpleTypeReference(value.getDeclaringClass().getName());
            this.name = value.name();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final SimpleEnumConstantReference other = (SimpleEnumConstantReference) obj;
            return Objects.equals(this.type, other.type)
                    && Objects.equals(this.name, other.name);
        }

        @Override
        public String toString() {
            return type + "." + name;
        }

        static List<SimpleEnumConstantReference> allOf(Enum<?>[] values) {
            ImmutableList.Builder<SimpleEnumConstantReference> result = ImmutableList.builder();
            for (Enum<?> value : values) {
                result.add(new SimpleEnumConstantReference(value));
            }
            return result.build();
        }
    }
}
