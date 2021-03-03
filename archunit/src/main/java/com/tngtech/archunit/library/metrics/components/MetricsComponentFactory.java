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
package com.tngtech.archunit.library.metrics.components;

import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Predicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;

import static java.util.Collections.singleton;

public class MetricsComponentFactory {
    public static MetricsComponents<JavaClass> fromPackages(Set<JavaPackage> packages) {
        ImmutableSet.Builder<MetricsComponent<JavaClass>> components = ImmutableSet.builder();
        for (JavaPackage javaPackage : packages) {
            components.add(MetricsComponent.of(javaPackage.getName(), filterRelevant(javaPackage.getAllClasses()), new Function<JavaClass, Set<MetricsElementDependency<JavaClass>>>() {
                @Override
                public Set<MetricsElementDependency<JavaClass>> apply(JavaClass input) {
                    ImmutableSet.Builder<MetricsElementDependency<JavaClass>> dependenciesBuilder = ImmutableSet.builder();
                    for (Dependency dependency : input.getDirectDependenciesFromSelf()) {
                        dependenciesBuilder.add(MetricsElementDependency.of(dependency.getOriginClass(), dependency.getTargetClass()));
                    }
                    return dependenciesBuilder.build();
                }
            }));
        }
        return MetricsComponents.of(components.build());
    }

    public static MetricsComponents<JavaClass> fromClasses(Iterable<JavaClass> javaClasses, final Predicate<JavaClass> includeDependencies) {
        ImmutableSet.Builder<MetricsComponent<JavaClass>> components = ImmutableSet.builder();
        for (JavaClass javaClass : filterRelevant(javaClasses)) {
            components.add(MetricsComponent.of(
                    javaClass.getName(),
                    javaClass.getName().replaceAll(".*\\.", ""),
                    singleton(javaClass),
                    new Function<JavaClass, Set<MetricsElementDependency<JavaClass>>>() {
                        @Override
                        public Set<MetricsElementDependency<JavaClass>> apply(JavaClass input) {
                            ImmutableSet.Builder<MetricsElementDependency<JavaClass>> dependenciesBuilder = ImmutableSet.builder();
                            for (Dependency dependency : input.getDirectDependenciesFromSelf()) {
                                if (includeDependencies.apply(dependency.getTargetClass())) {
                                    dependenciesBuilder.add(MetricsElementDependency.of(dependency.getOriginClass(), dependency.getTargetClass()));
                                }
                            }
                            return dependenciesBuilder.build();
                        }
                    }));
        }
        return MetricsComponents.of(components.build());
    }

    private static Set<JavaClass> filterRelevant(Iterable<JavaClass> classes) {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        for (JavaClass javaClass : classes) {
            if (!javaClass.getSimpleName().equals("package-info")) {
                result.add(javaClass);
            }
        }
        return result.build();
    }
}
