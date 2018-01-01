/*
 * Copyright 2018 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.junit.ReflectionUtils.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.junit.ArchTestExecution.elementShouldBeIgnored;
import static com.tngtech.archunit.junit.ReflectionUtils.getAllFields;
import static com.tngtech.archunit.junit.ReflectionUtils.getAllMethods;

public final class ArchRules {
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

    @PublicAPI(usage = ACCESS)
    public static ArchRules in(Class<?> definitionLocation) {
        return new ArchRules(definitionLocation);
    }

    Set<ArchTestExecution> asTestExecutions(boolean forceIgnore) {
        ImmutableSet.Builder<ArchTestExecution> result = ImmutableSet.builder();
        for (Field field : fields) {
            result.addAll(archRuleExecutionsOf(field, forceIgnore));
        }
        for (Method method : methods) {
            result.add(new ArchTestMethodExecution(method.getDeclaringClass(), method, forceIgnore));
        }
        return result.build();
    }

    private Set<ArchTestExecution> archRuleExecutionsOf(Field field, boolean forceIgnore) {
        return ArchRules.class.isAssignableFrom(field.getType()) ?
                getArchRulesIn(field).asTestExecutions(forceIgnore || elementShouldBeIgnored(field)) :
                Collections.<ArchTestExecution>singleton(new ArchRuleExecution(field.getDeclaringClass(), field, forceIgnore));
    }

    private ArchRules getArchRulesIn(Field field) {
        try {
            ArchRules value = (ArchRules) field.get(null);
            return checkNotNull(value, "Field %s.%s is not initialized",
                    field.getDeclaringClass().getName(), field.getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
