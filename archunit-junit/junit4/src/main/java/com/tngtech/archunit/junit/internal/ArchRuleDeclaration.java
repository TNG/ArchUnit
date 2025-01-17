/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.junit.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTests;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.junit.internal.ArchTestExecution.getValue;
import static com.tngtech.archunit.junit.internal.ReflectionUtils.getAllFields;
import static com.tngtech.archunit.junit.internal.ReflectionUtils.getAllMethods;
import static com.tngtech.archunit.junit.internal.ReflectionUtils.withAnnotation;
import static java.util.Collections.singleton;

abstract class ArchRuleDeclaration<T extends AnnotatedElement> {
    final List<Class<?>> testClassPath;
    final T declaration;
    final Class<?> owner;
    private final boolean forceIgnore;

    ArchRuleDeclaration(List<Class<?>> testClassPath, T declaration, Class<?> owner, boolean forceIgnore) {
        this.testClassPath = testClassPath;
        this.declaration = declaration;
        this.owner = owner;
        this.forceIgnore = forceIgnore;
    }

    abstract void handleWith(Handler handler);

    private static ArchRuleDeclaration<Method> from(List<Class<?>> testClassPath, Method method, Class<?> methodOwner, boolean forceIgnore) {
        return new AsMethod(testClassPath, method, methodOwner, forceIgnore);
    }

    private static ArchRuleDeclaration<Field> from(List<Class<?>> testClassPath, Field field, Class<?> fieldOwner, boolean forceIgnore) {
        return new AsField(testClassPath, field, fieldOwner, forceIgnore);
    }

    static boolean elementShouldBeIgnored(Class<?> owner, AnnotatedElement ruleDeclaration) {
        return owner.getAnnotation(ArchIgnore.class) != null ||
                ruleDeclaration.getAnnotation(ArchIgnore.class) != null;
    }

    boolean shouldBeIgnored() {
        return forceIgnore || elementShouldBeIgnored(owner, declaration);
    }

    static Set<ArchRuleDeclaration<?>> toDeclarations(
            ArchTests archTests, List<Class<?>> testClassPath, Class<? extends Annotation> archTestAnnotationType, boolean forceIgnore) {

        ImmutableSet.Builder<ArchRuleDeclaration<?>> result = ImmutableSet.builder();
        Class<?> definitionLocation = archTests.getDefinitionLocation();
        List<Class<?>> childTestClassPath = ImmutableList.<Class<?>>builder().addAll(testClassPath).add(definitionLocation).build();
        for (Field field : getAllFields(definitionLocation, withAnnotation(archTestAnnotationType))) {
            result.addAll(archRuleDeclarationsFrom(childTestClassPath, field, definitionLocation, archTestAnnotationType, forceIgnore));
        }
        for (Method method : getAllMethods(definitionLocation, withAnnotation(archTestAnnotationType))) {
            result.add(ArchRuleDeclaration.from(childTestClassPath, method, definitionLocation, forceIgnore));
        }
        return result.build();
    }

    private static Set<ArchRuleDeclaration<?>> archRuleDeclarationsFrom(List<Class<?>> testClassPath, Field field, Class<?> fieldOwner,
            Class<? extends Annotation> archTestAnnotationType, boolean forceIgnore) {

        return ArchTests.class.isAssignableFrom(field.getType()) ?
                toDeclarations(getArchTestsIn(field, fieldOwner), testClassPath, archTestAnnotationType, forceIgnore || elementShouldBeIgnored(fieldOwner, field)) :
                singleton(ArchRuleDeclaration.from(testClassPath, field, fieldOwner, forceIgnore));
    }

    private static ArchTests getArchTestsIn(Field field, Class<?> fieldOwner) {
        ArchTests value = getValue(field, fieldOwner);
        return checkNotNull(value, "Field %s.%s is not initialized", fieldOwner.getName(), field.getName());
    }

    private static class AsMethod extends ArchRuleDeclaration<Method> {
        AsMethod(List<Class<?>> testClassPath, Method method, Class<?> methodOwner, boolean forceIgnore) {
            super(testClassPath, method, methodOwner, forceIgnore);
        }

        @Override
        void handleWith(Handler handler) {
            handler.handleMethodDeclaration(testClassPath, declaration, owner, shouldBeIgnored());
        }
    }

    private static class AsField extends ArchRuleDeclaration<Field> {
        AsField(List<Class<?>> testClassPath, Field field, Class<?> fieldOwner, boolean forceIgnore) {
            super(testClassPath, field, fieldOwner, forceIgnore);
        }

        @Override
        void handleWith(Handler handler) {
            handler.handleFieldDeclaration(testClassPath, declaration, owner, shouldBeIgnored());
        }
    }

    interface Handler {
        void handleFieldDeclaration(List<Class<?>> testClassPath, Field field, Class<?> fieldOwner, boolean ignore);

        void handleMethodDeclaration(List<Class<?>> testClassPath, Method method, Class<?> methodOwner, boolean ignore);
    }
}
