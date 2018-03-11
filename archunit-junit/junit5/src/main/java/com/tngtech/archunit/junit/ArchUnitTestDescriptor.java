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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

import static com.tngtech.archunit.junit.ReflectionUtils.getAllFields;
import static com.tngtech.archunit.junit.ReflectionUtils.getAllMethods;
import static com.tngtech.archunit.junit.ReflectionUtils.getValue;
import static com.tngtech.archunit.junit.ReflectionUtils.withAnnotation;

public class ArchUnitTestDescriptor extends AbstractTestDescriptor implements Node<ArchUnitEngineExecutionContext> {
    ArchUnitTestDescriptor(UniqueId uniqueId, Class<?> testClass) {
        super(uniqueId.append("class", testClass.getName()), testClass.getSimpleName());

        getAllFields(testClass, withAnnotation(ArchTest.class))
                .forEach(field -> addChild(descriptorFor(getUniqueId(), testClass, field)));
        getAllMethods(testClass, withAnnotation(ArchTest.class))
                .forEach(method -> addChild(new ArchUnitMethodDescriptor(getUniqueId(), method, null)));
    }

    private static TestDescriptor descriptorFor(UniqueId uniqueId, Class<?> testClass, Field field) {
        uniqueId = uniqueId.append("field", field.getName());
        return ArchRules.class.isAssignableFrom(field.getType())
                ? new ArchUnitRulesDescriptor(uniqueId, getDeclaredRules(testClass, field), null)
                : new ArchUnitRuleDescriptor(uniqueId, getValue(field, null), null);
    }

    private static DeclaredArchRules getDeclaredRules(Class<?> testClass, Field field) {
        return new DeclaredArchRules(testClass, getValue(field, null));
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    private static class ArchUnitRuleDescriptor extends AbstractTestDescriptor implements Node<ArchUnitEngineExecutionContext> {
        private final ArchRule rule;
        private final JavaClasses classes;

        ArchUnitRuleDescriptor(UniqueId uniqueId, ArchRule rule, JavaClasses classes) {
            super(uniqueId.append("rule", rule.getDescription()), rule.getDescription());
            this.rule = rule;
            this.classes = classes;
        }

        @Override
        public Type getType() {
            return Type.TEST;
        }
    }

    private static class ArchUnitMethodDescriptor extends AbstractTestDescriptor implements Node<ArchUnitEngineExecutionContext> {
        private final Method method;
        private final JavaClasses classes;

        ArchUnitMethodDescriptor(UniqueId uniqueId, Method method, JavaClasses classes) {
            super(uniqueId.append("method", method.getName()), method.getName());
            this.method = method;
            this.classes = classes;
        }

        @Override
        public Type getType() {
            return Type.TEST;
        }
    }

    private static class ArchUnitRulesDescriptor extends AbstractTestDescriptor implements Node<ArchUnitEngineExecutionContext> {

        ArchUnitRulesDescriptor(UniqueId uniqueId, DeclaredArchRules rules, JavaClasses classes) {
            super(uniqueId.append("class", rules.getDefinitionLocation()), rules.getDisplayName());
            rules.forEachDeclaration(declaration -> declaration.handleWith(new ArchRuleDeclaration.Handler() {
                @Override
                public void handleFieldDeclaration(Field field, boolean ignore) {
                    addChild(descriptorFor(getUniqueId(), rules.getTestClass(), field));
                }

                @Override
                public void handleMethodDeclaration(Method method, boolean ignore) {
                    addChild(new ArchUnitMethodDescriptor(getUniqueId(), method, classes));
                }
            }));
        }

        @Override
        public Type getType() {
            return Type.CONTAINER;
        }
    }

    private static class DeclaredArchRules {
        private final Class<?> testClass;
        private final ArchRules rules;

        DeclaredArchRules(Class<?> testClass, ArchRules rules) {
            this.testClass = testClass;
            this.rules = rules;
        }

        Class<?> getTestClass() {
            return testClass;
        }

        String getDefinitionLocation() {
            return rules.getDefinitionLocation().getName();
        }

        String getDisplayName() {
            return rules.getDefinitionLocation().getSimpleName();
        }

        void forEachDeclaration(Consumer<ArchRuleDeclaration<?>> doWithDeclaration) {
            rules.asDeclarations(testClass, false).forEach(doWithDeclaration);
        }
    }
}
