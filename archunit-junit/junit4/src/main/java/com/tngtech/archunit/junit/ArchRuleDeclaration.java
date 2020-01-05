/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.junit.ArchTestExecution.getValue;
import static com.tngtech.archunit.junit.ReflectionUtils.getAllFields;
import static com.tngtech.archunit.junit.ReflectionUtils.getAllMethods;
import static com.tngtech.archunit.junit.ReflectionUtils.withAnnotation;

abstract class ArchRuleDeclaration<T extends AnnotatedElement> {
    private final Class<?> testClass;
    final T declaration;
    final Class<?> owner;
    private final boolean forceIgnore;

    ArchRuleDeclaration(Class<?> testClass, T declaration, Class<?> owner, boolean forceIgnore) {
        this.testClass = testClass;
        this.declaration = declaration;
        this.owner = owner;
        this.forceIgnore = forceIgnore;
    }

    abstract void handleWith(Handler handler);

    private static ArchRuleDeclaration<Method> from(Class<?> testClass, Method method, Class<?> methodOwner, boolean forceIgnore) {
        return new AsMethod(testClass, method, methodOwner, forceIgnore);
    }

    private static ArchRuleDeclaration<Field> from(Class<?> testClass, Field field, Class<?> fieldOwner, boolean forceIgnore) {
        return new AsField(testClass, field, fieldOwner, forceIgnore);
    }

    static <T extends AnnotatedElement & Member> boolean elementShouldBeIgnored(T member) {
        return elementShouldBeIgnored(member.getDeclaringClass(), member);
    }

    static boolean elementShouldBeIgnored(Class<?> testClass, AnnotatedElement ruleDeclaration) {
        return testClass.getAnnotation(ArchIgnore.class) != null ||
                ruleDeclaration.getAnnotation(ArchIgnore.class) != null;
    }

    boolean shouldBeIgnored() {
        return forceIgnore || elementShouldBeIgnored(testClass, declaration);
    }

    static Set<ArchRuleDeclaration<?>> toDeclarations(
            ArchRules rules, Class<?> testClass, Class<? extends Annotation> archTestAnnotationType, boolean forceIgnore) {

        ImmutableSet.Builder<ArchRuleDeclaration<?>> result = ImmutableSet.builder();
        Class<?> definitionLocation = rules.getDefinitionLocation();
        for (Field field : getAllFields(definitionLocation, withAnnotation(archTestAnnotationType))) {
            result.addAll(archRuleDeclarationsFrom(testClass, field, definitionLocation, archTestAnnotationType, forceIgnore));
        }
        for (Method method : getAllMethods(definitionLocation, withAnnotation(archTestAnnotationType))) {
            result.add(ArchRuleDeclaration.from(testClass, method, definitionLocation, forceIgnore));
        }
        return result.build();
    }

    private static Set<ArchRuleDeclaration<?>> archRuleDeclarationsFrom(Class<?> testClass, Field field, Class<?> fieldOwner,
            Class<? extends Annotation> archTestAnnotationType, boolean forceIgnore) {

        return ArchRules.class.isAssignableFrom(field.getType()) ?
                toDeclarations(getArchRulesIn(field, fieldOwner), testClass, archTestAnnotationType, forceIgnore || elementShouldBeIgnored(field)) :
                Collections.<ArchRuleDeclaration<?>>singleton(ArchRuleDeclaration.from(testClass, field, fieldOwner, forceIgnore));
    }

    private static ArchRules getArchRulesIn(Field field, Class<?> fieldOwner) {
        ArchRules value = getValue(field, fieldOwner);
        return checkNotNull(value, "Field %s.%s is not initialized", fieldOwner.getName(), field.getName());
    }

    private static class AsMethod extends ArchRuleDeclaration<Method> {
        AsMethod(Class<?> testClass, Method method, Class<?> methodOwner, boolean forceIgnore) {
            super(testClass, method, methodOwner, forceIgnore);
        }

        @Override
        void handleWith(Handler handler) {
            handler.handleMethodDeclaration(declaration, owner, shouldBeIgnored());
        }
    }

    private static class AsField extends ArchRuleDeclaration<Field> {
        AsField(Class<?> testClass, Field field, Class<?> fieldOwner, boolean forceIgnore) {
            super(testClass, field, fieldOwner, forceIgnore);
        }

        @Override
        void handleWith(Handler handler) {
            handler.handleFieldDeclaration(declaration, owner, shouldBeIgnored());
        }
    }

    interface Handler {
        void handleFieldDeclaration(Field field, Class<?> fieldOwner, boolean ignore);

        void handleMethodDeclaration(Method method, Class<?> methodOwner, boolean ignore);
    }
}
