/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@PublicAPI(usage = ACCESS)
public final class ProxyRules {
    private ProxyRules() {
    }

    @PublicAPI(usage = ACCESS)
    public static ArchRule no_classes_should_directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Class<? extends Annotation> annotationType) {
        return no_classes_should_directly_call_other_methods_declared_in_the_same_class_that(are(annotatedWith(annotationType)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchRule no_classes_should_directly_call_other_methods_declared_in_the_same_class_that(DescribedPredicate<? super MethodCallTarget> predicate) {
        return noClasses().should(directly_call_other_methods_declared_in_the_same_class_that(predicate))
                .because("it bypasses the proxy mechanism");
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> directly_call_other_methods_declared_in_the_same_class_that_are_annotated_with(Class<? extends Annotation> annotationType) {
        return directly_call_other_methods_declared_in_the_same_class_that(are(annotatedWith(annotationType)));
    }

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> directly_call_other_methods_declared_in_the_same_class_that(final DescribedPredicate<? super MethodCallTarget> predicate) {
        return new ArchCondition<JavaClass>("directly call other methods declared in the same class that " + predicate.getDescription()) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaMethodCall call : javaClass.getMethodCallsFromSelf()) {
                    boolean satisfied = call.getOriginOwner().equals(call.getTargetOwner()) && predicate.apply(call.getTarget());
                    events.add(new SimpleConditionEvent(call, satisfied, call.getDescription()));
                }
            }
        };
    }
}
