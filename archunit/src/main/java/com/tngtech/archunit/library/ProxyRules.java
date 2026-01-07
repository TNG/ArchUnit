/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaModifier.BRIDGE;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ProxyRules provides a set of general {@link ArchCondition ArchConditions}
 * and {@link ArchRule ArchRules} for checking the usage of proxies.
 */
@PublicAPI(usage = ACCESS)
public final class ProxyRules {
    private ProxyRules() {
    }

    /**
     * Returns a rule that checks that none of the given classes directly calls
     * other methods declared in the same class that are annotated with the
     * given annotation (ignoring calls from synthetic bridge methods).
     *
     * <p>
     * As an example, the Spring Framework handles transactions by creating a proxy.
     * This proxy does the actual transaction management every time a method
     * annotated with {@code @Transactional} is invoked.
     * <pre>{@code
     * class FooEndpoint {
     *     void updateFooName(String name) {
     *         // ...
     *         fooService.update(foo); // Spring proxies this method call to do the actual transaction management
     *     }
     * }
     *
     * class FooService {
     *     @Transactional
     *     void update(Foo foo) {
     *         // ... does something
     *     }
     * }
     * }</pre>
     * However, this does not work if the method call comes from the same class.
     * <pre>{@code
     * class FooService {
     *     void updateFooName(String name) {
     *         // ...
     *         update(foo); // Spring does not proxy this method call and does not start a transaction
     *     }
     *
     *     @Transactional
     *     void update(Foo foo) {
     *         // ... does something
     *     }
     * }
     * }</pre>
     * This case could be checked with
     * <pre>{@code
     * no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Transactional.class)
     * }</pre>
     * </p>
     */
    @PublicAPI(usage = ACCESS)
    public static ArchRule no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Class<? extends Annotation> annotationType) {
        return no_classes_should_directly_call_other_methods_declared_in_the_same_class_that(are(annotatedWith(annotationType)));
    }

    /**
     * Returns a rule that checks that none of the given classes directly calls
     * other methods declared in the same class that matches the given predicate
     * (ignoring calls from synthetic bridge methods).
     *
     * <p>
     * For an example, see {@link #no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Class)}
     * </p>
     */
    @PublicAPI(usage = ACCESS)
    public static ArchRule no_classes_should_directly_call_other_methods_declared_in_the_same_class_that(DescribedPredicate<? super MethodCallTarget> predicate) {
        return noClasses().should(directly_call_other_methods_declared_in_the_same_class_that(predicate))
                .because("it bypasses the proxy mechanism");
    }

    /**
     * Returns a condition that matches classes that directly calls other methods
     * declared in the same class that are annotated with the given annotation
     * (ignoring calls from synthetic bridge methods).
     * <p>
     * As an example, the Spring Framework handles transactions by creating a proxy.
     * This proxy does the actual transaction management every time a method
     * annotated with {@code @Transactional} is invoked.
     * <pre>{@code
     * class FooEndpoint {
     *     void updateFooName(String name) {
     *         // ...
     *         fooService.update(foo); // Spring proxies this method call to do the actual transaction management
     *     }
     * }
     *
     * class FooService {
     *     @Transactional
     *     void update(Foo foo) {
     *         // ... does something
     *     }
     * }
     * }</pre>
     * However, this does not work if the method call comes from the same class.
     * <pre>{@code
     * class FooService {
     *     void updateFooName(String name) {
     *         // ...
     *         update(foo); // Spring does not proxy this method call and does not start a transaction
     *     }
     *
     *     @Transactional
     *     void update(Foo foo) {
     *         // ... does something
     *     }
     * }
     * }</pre>
     * The condition
     * <pre>{@code
     * directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Transactional.class)
     * }</pre>
     * matches the second example.
     * </p>
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Class<? extends Annotation> annotationType) {
        return directly_call_other_methods_declared_in_the_same_class_that(are(annotatedWith(annotationType)));
    }

    /**
     * Returns a condition that matches classes that directly calls other methods
     * declared in the same class that matches the given predicate
     * (ignoring calls from synthetic bridge methods).
     * <p>
     * For an example, see {@link #directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Class)}
     * </p>
     */
    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> directly_call_other_methods_declared_in_the_same_class_that(DescribedPredicate<? super MethodCallTarget> predicate) {
        return new ArchCondition<JavaClass>("directly call other methods declared in the same class that " + predicate.getDescription()) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getMethodCallsFromSelf().stream()
                        .filter(Get.origin().is(not(modifier(BRIDGE))))
                        .forEach(call -> {
                            boolean satisfied = call.getOriginOwner().equals(call.getTargetOwner()) && predicate.test(call.getTarget());
                            events.add(new SimpleConditionEvent(call, satisfied, call.getDescription()));
                        });
            }
        };
    }
}
