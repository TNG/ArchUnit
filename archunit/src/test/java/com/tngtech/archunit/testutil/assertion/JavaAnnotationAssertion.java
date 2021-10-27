package com.tngtech.archunit.testutil.assertion;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ThrowableAssert;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatAnnotation;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.TestUtils.invoke;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JavaAnnotationAssertion extends AbstractObjectAssert<JavaAnnotationAssertion, JavaAnnotation<?>> {
    public JavaAnnotationAssertion(JavaAnnotation<?> actual) {
        super(actual, JavaAnnotationAssertion.class);
    }

    public JavaAnnotationAssertion matches(Annotation annotation) {
        assertThat(runtimePropertiesOf(singleton(actual)))
                .as("runtime properties of " + actual)
                .containsExactly(propertiesOf(annotation));
        return this;
    }

    public JavaAnnotationAssertion hasType(Class<? extends Annotation> annotationType) {
        assertThatType(actual.getRawType())
                .as("annotation type of " + descriptionText())
                .matches(annotationType);
        return this;
    }

    public JavaAnnotationAssertion hasStringProperty(String propertyName, String expectedValue) {
        assertThat(getPropertyOfType(propertyName, String.class))
                .as(annotationPropertyDescription("String", propertyName))
                .isEqualTo(expectedValue);
        return this;
    }

    public JavaAnnotationAssertion hasClassProperty(String propertyName, Class<?> expectedClass) {
        JavaClass actualClassValue = getPropertyOfType(propertyName, JavaClass.class);
        assertThatType(actualClassValue).as(annotationPropertyDescription("Class<?>", propertyName)).matches(expectedClass);
        return this;
    }

    public JavaAnnotationAssertion hasEnumProperty(String propertyName, Enum<?> expectedEnumConstant) {
        JavaEnumConstant actualEnumConstant = getPropertyOfType(propertyName, JavaEnumConstant.class);
        assertThat(actualEnumConstant)
                .as(annotationPropertyDescription(actualEnumConstant.getDeclaringClass().getSimpleName(), propertyName))
                .isEquivalentTo(expectedEnumConstant);
        return this;
    }

    public JavaAnnotationAssertion hasAnnotationProperty(String propertyName, AnnotationPropertyAssertion propertyAssertion) {
        JavaAnnotation<?> actualAnnotationProperty = getPropertyOfType(propertyName, JavaAnnotation.class);
        propertyAssertion.check(actual, propertyName, actualAnnotationProperty);
        return this;
    }

    public JavaAnnotationAssertion hasExplicitlyDeclaredStringProperty(String propertyName, String expectedValue) {
        String description = annotationPropertyDescription("String", propertyName);
        assertThat(actual.hasExplicitlyDeclaredProperty(propertyName))
                .as(description + " has explicitly declared value")
                .isTrue();
        assertThat(actual.getExplicitlyDeclaredProperty(propertyName)).as(description).isEqualTo(expectedValue);
        assertThat(actual.tryGetExplicitlyDeclaredProperty(propertyName)).as(description).contains(expectedValue);
        return this;
    }

    public JavaAnnotationAssertion hasNoExplicitlyDeclaredProperty(final String propertyName) {
        String description = annotationPropertyDescription("String", propertyName);
        assertThat(actual.hasExplicitlyDeclaredProperty(propertyName))
                .as(description + " has explicitly declared value")
                .isFalse();
        assertThat(actual.tryGetExplicitlyDeclaredProperty(propertyName)).as(description).isAbsent();
        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                actual.getExplicitlyDeclaredProperty(propertyName);
            }
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("%s has no explicitly declared property '%s'", actual.getDescription(), propertyName);
        return this;
    }

    @SuppressWarnings("unchecked")
    private <T> T getPropertyOfType(String propertyName, Class<T> propertyType) {
        Optional<?> property = actual.get(propertyName);
        assertThat(property).as("property '%s'", propertyName).isPresent();
        assertThat(property.get()).as("property '%s'", propertyName).isInstanceOf(propertyType);
        return (T) property.get();
    }

    private String annotationPropertyDescription(String typeName, String propertyName) {
        return String.format("%s @%s.%s()", typeName, actual.getRawType().getSimpleName(), propertyName)
                + (Strings.isNullOrEmpty(descriptionText()) ? "" : " of " + descriptionText());
    }

    @SuppressWarnings("rawtypes")
    public static Set<Map<String, Object>> runtimePropertiesOf(Set<? extends JavaAnnotation<?>> annotations) {
        List<Annotation> converted = new ArrayList<>();
        for (JavaAnnotation<?> annotation : annotations) {
            Annotation reflectionAnnotation = annotation.as((Class) annotation.getRawType().reflect());
            if (isRetentionRuntime(reflectionAnnotation)) {
                converted.add(reflectionAnnotation);
            }
        }
        return propertiesOf(converted.toArray(new Annotation[0]));
    }

    private static boolean isRetentionRuntime(Annotation annotation) {
        return annotation.annotationType().isAnnotationPresent(Retention.class)
                && annotation.annotationType().getAnnotation(Retention.class).value() == RUNTIME;
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

    public static AnnotationPropertyAssertion annotationProperty() {
        return new AnnotationPropertyAssertion();
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

    public static class AnnotationPropertyAssertion {
        private final List<Consumer<JavaAnnotationAssertion>> executeAssertions = new ArrayList<>();

        public AnnotationPropertyAssertion withAnnotationType(final Class<? extends Annotation> annotationType) {
            executeAssertions.add(new Consumer<JavaAnnotationAssertion>() {
                @Override
                public void accept(JavaAnnotationAssertion assertion) {
                    assertion.hasType(annotationType);
                }
            });
            return this;
        }

        public AnnotationPropertyAssertion withClassProperty(final String propertyName, final Class<?> propertyValue) {
            executeAssertions.add(new Consumer<JavaAnnotationAssertion>() {
                @Override
                public void accept(JavaAnnotationAssertion assertion) {
                    assertion.hasClassProperty(propertyName, propertyValue);
                }
            });
            return this;
        }

        void check(JavaAnnotation<?> owner, String propertyName, JavaAnnotation<?> actualAnnotationProperty) {
            JavaAnnotationAssertion assertion = assertThatAnnotation(actualAnnotationProperty)
                    .as("%s @%s.%s()", actualAnnotationProperty.getRawType().getSimpleName(), owner.getRawType().getSimpleName(), propertyName);
            for (Consumer<JavaAnnotationAssertion> executeAssertion : executeAssertions) {
                executeAssertion.accept(assertion);
            }
        }

        private interface Consumer<T> {
            void accept(T value);
        }
    }
}
