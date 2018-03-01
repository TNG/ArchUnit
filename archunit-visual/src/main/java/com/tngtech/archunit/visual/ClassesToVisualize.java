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
package com.tngtech.archunit.visual;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

class ClassesToVisualize {
    private Map<String, JavaClass> classes = new HashMap<>();
    private Map<String, JavaClass> innerClasses = new HashMap<>();
    private Map<String, JavaClass> dependenciesClasses = new HashMap<>();
    private Map<String, JavaClass> dependenciesInnerClasses = new HashMap<>();

    private ClassesToVisualize(JavaClasses classes, VisualizationContext context) {
        Set<JavaClass> includedClasses = context.filterIncluded(classes);
        addClasses(includedClasses);
        addDependencies(context);
    }

    private void addClasses(Set<JavaClass> classes) {
        for (JavaClass clazz : classes) {
            if (!clazz.getPackage().isEmpty()) {
                if (clazz.getEnclosingClass().isPresent()) {
                    if (clazz.getSimpleName().endsWith("C2")) {
                    }
                    innerClasses.put(clazz.getName(), clazz);
                } else if (!clazz.getSimpleName().isEmpty()) {
                    this.classes.put(clazz.getName(), clazz);
                }
            }
        }
    }

    private void addDependencies(VisualizationContext context) {
        for (JavaClass clazz : Iterables.concat(getClasses(), getInnerClasses())) {

            boolean f = false;
            if (clazz.getName().endsWith(".io.Files")) {
                f = true;
            }
            //for (JavaClass clazz : getClasses()) {
            for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                if (context.isElementIncluded(dependency.getTargetClass()) && !dependency.getTargetClass().getPackage().isEmpty() &&
                        !classes.keySet().contains(dependency.getTargetClass().getName()) &&
                        !innerClasses.keySet().contains(dependency.getTargetClass().getName())) {
                    if (dependency.getTargetClass().getName().endsWith("JavaInnerClassTestInnerClass")) {
                    }
                    if (dependency.getTargetClass().getName().contains("$")) {
                        //FIXME: it is maybe necessary to add an inner class's parent class, if there is only a dependency to the inner class
                        dependenciesInnerClasses.put(dependency.getTargetClass().getName(), dependency.getTargetClass());
                    }
                    else if (!dependency.getTargetClass().getSimpleName().isEmpty()) {
                        dependenciesClasses.put(dependency.getTargetClass().getName(), dependency.getTargetClass());
                    }
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

    Iterable<JavaClass> getDependenciesClasses() {
        return dependenciesClasses.values();
    }

    Iterable<JavaClass> getDependenciesInnerClasses() {
        return dependenciesInnerClasses.values();
    }

    Iterable<JavaClass> getDependencies() {
        return Iterables.concat(getDependenciesClasses(), getDependenciesInnerClasses());
    }

    Set<String> getPackages() {
        Set<String> result = new HashSet<>();
        for (JavaClass c : getAll()) {
            if (c.getPackage().isEmpty()) {
                System.out.println(c.getName());
                System.out.println("Sollte nicht erreicht werden...");
            }
            result.add(c.getPackage());
        }
        return result;
    }

    Iterable<JavaClass> getAll() {
        return Iterables.concat(getClasses(), getInnerClasses(), getDependenciesClasses(), getDependenciesInnerClasses());
    }

    static ClassesToVisualize from(JavaClasses classes, VisualizationContext context) {
        return new ClassesToVisualize(classes, context);
    }
}
