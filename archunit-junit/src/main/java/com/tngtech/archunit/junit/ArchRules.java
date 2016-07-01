package com.tngtech.archunit.junit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import static com.tngtech.archunit.core.ReflectionUtils.getAllFields;
import static com.tngtech.archunit.core.ReflectionUtils.getAllMethods;
import static com.tngtech.archunit.core.ReflectionUtils.withAnnotation;

public class ArchRules<T> {
    private final Collection<Field> fields;
    private final Collection<Method> methods;

    @SuppressWarnings("unchecked")
    public ArchRules(Class<?> definitionLocation) {
        fields = getAllFields(definitionLocation, withAnnotation(ArchTest.class));
        methods = getAllMethods(definitionLocation, withAnnotation(ArchTest.class));
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
