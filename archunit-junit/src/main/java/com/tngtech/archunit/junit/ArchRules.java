package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.ReflectionUtils.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.ReflectionUtils.getAllFields;
import static com.tngtech.archunit.core.ReflectionUtils.getAllMethods;

public class ArchRules<T> {
    private final Collection<Field> fields;
    private final Collection<Method> methods;

    @SuppressWarnings("unchecked")
    private ArchRules(Class<?> definitionLocation) {
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
            result.addAll(archRuleExecutionsOf(field));
        }
        for (Method method : methods) {
            result.add(new ArchTestMethodExecution(method.getDeclaringClass(), method));
        }
        return result.build();
    }

    private Set<ArchTestExecution> archRuleExecutionsOf(Field field) {
        return ArchRules.class.isAssignableFrom(field.getType()) ?
                getArchRulesIn(field).asTestExecutions() :
                Collections.<ArchTestExecution>singleton(new ArchRuleExecution(field.getDeclaringClass(), field));
    }

    private ArchRules<?> getArchRulesIn(Field field) {
        try {
            ArchRules value = (ArchRules) field.get(null);
            return checkNotNull(value, "Field %s.%s is not initialized",
                    field.getDeclaringClass().getName(), field.getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
