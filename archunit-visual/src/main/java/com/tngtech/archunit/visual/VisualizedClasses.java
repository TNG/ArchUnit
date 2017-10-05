/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.visual;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

class VisualizedClasses {
    private Map<String, JavaClass> classes = new HashMap<>();
    private Map<String, JavaClass> innerClasses = new HashMap<>();
    private Map<String, JavaClass> dependencies = new HashMap<>();

    private VisualizedClasses(JavaClasses classes, VisualizationContext context) {
        Set<JavaClass> includedClasses = context.filterIncluded(classes);
        addClasses(includedClasses);
        addDependencies(context);
    }

    private void addClasses(Set<JavaClass> classes) {
        for (JavaClass clazz : classes) {
            if (clazz.getEnclosingClass().isPresent()) {
                innerClasses.put(clazz.getName(), clazz);
            } else if (!clazz.getSimpleName().isEmpty()) {
                this.classes.put(clazz.getName(), clazz);
            }
        }
    }

    private void addDependencies(VisualizationContext context) {
        for (JavaClass clazz : classes.values()) {
            for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                if (context.isElementIncluded(dependency.getTargetClass()) &&
                        !classes.keySet().contains(dependency.getTargetClass().getName()) &&
                        !innerClasses.keySet().contains(dependency.getTargetClass().getName())) {
                    dependencies.put(dependency.getTargetClass().getName(), dependency.getTargetClass());
                }
            }
        }
    }

    Iterable<JavaClass> getClasses() {
        return classes.values();
    }

    Iterable<JavaClass> getInnerClasses() {
        return innerClasses.values();
    }

    Iterable<JavaClass> getDependencies() {
        return dependencies.values();
    }

    Set<String> getPackages() {
        Set<String> result = new HashSet<>();
        for (JavaClass c : getAll()) {
            result.add(c.getPackage());
        }
        return result;
    }

    Iterable<JavaClass> getAll() {
        return Iterables.concat(getClasses(), getInnerClasses(), getDependencies());
    }

    static VisualizedClasses from(JavaClasses classes, VisualizationContext context) {
        return new VisualizedClasses(classes, context);
    }
}
