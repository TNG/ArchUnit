package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.ReflectionUtils.Predicate;

import static com.tngtech.archunit.core.ReflectionUtils.getAllFields;
import static com.tngtech.archunit.core.ReflectionUtils.getAllMethods;

public class ArchRules<T> {
    private final Collection<Field> fields;
    private final Collection<Method> methods;

    @SuppressWarnings("unchecked")
    public ArchRules(Class<?> definitionLocation) {
        fields = getAllFields(definitionLocation, withAnnotation(ArchTest.class));
        methods = getAllMethods(definitionLocation, withAnnotation(ArchTest.class));
    }

    private static Predicate<AnnotatedElement> withAnnotation(final Class<? extends Annotation> annotationType) {
        return new Predicate<AnnotatedElement>() {
            @Override
            public boolean apply(AnnotatedElement input) {
                return input.getAnnotation(annotationType) != null;
            }
        };
    }

    public static <T> ArchRules<T> in(Class<?> definitionLocation) {
        return new ArchRules<>(definitionLocation);
    }

    public Set<ArchTestExecution> asTestExecutions() {
        ImmutableSet.Builder<ArchTestExecution> result = ImmutableSet.builder();
        for (Field field : fields) {
            result.add(new ArchRuleExecution(field.getDeclaringClass(), field));
        }
        for (Method method : methods) {
            result.add(new ArchTestMethodExecution(method.getDeclaringClass(), method));
        }
        return result.build();
    }
}
