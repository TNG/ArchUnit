/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

class JavaClassTransitiveDependencies {
    private JavaClassTransitiveDependencies() {
    }

    static Set<Dependency> findTransitiveDependenciesFrom(JavaClass javaClass) {
        ImmutableSet.Builder<Dependency> transitiveDependencies = ImmutableSet.builder();
        Set<JavaClass> analyzedClasses = new HashSet<>();  // to avoid infinite recursion for cyclic dependencies
        addTransitiveDependenciesFrom(javaClass, transitiveDependencies, analyzedClasses);
        return transitiveDependencies.build();
    }

    private static void addTransitiveDependenciesFrom(JavaClass javaClass, ImmutableSet.Builder<Dependency> transitiveDependencies, Set<JavaClass> analyzedClasses) {
        analyzedClasses.add(javaClass);  // currently being analyzed
        Set<JavaClass> targetClassesToRecurse = new HashSet<>();
        for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
            transitiveDependencies.add(dependency);
            targetClassesToRecurse.add(dependency.getTargetClass().getBaseComponentType());
        }
        for (JavaClass targetClass : targetClassesToRecurse) {
            if (!analyzedClasses.contains(targetClass)) {
                addTransitiveDependenciesFrom(targetClass, transitiveDependencies, analyzedClasses);
            }
        }
    }
}
