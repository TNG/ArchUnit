package com.tngtech.archunit.junit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;

public class ArchRules<T> {
    private final Set<Field> fields;
    private final Set<Method> methods;

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
