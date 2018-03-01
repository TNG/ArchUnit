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

import java.util.*;

import com.google.common.collect.Iterables;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

class ClassesToVisualize {
    private ClassList classList = new ClassList();
    private ClassList dependenciesClassList = new ClassList();

    private ClassesToVisualize(JavaClasses classes, VisualizationContext context) {
        Set<JavaClass> includedClasses = context.filterIncluded(classes);
        addClasses(includedClasses);
        addDependencies(context);
    }

    private boolean isClassValid(JavaClass clazz) {
        return !clazz.getPackage().isEmpty() && !clazz.getSimpleName().isEmpty();
    }

    private void addClasses(Set<JavaClass> classes) {
        for (JavaClass clazz : classes) {
            if (!clazz.getPackage().isEmpty()) {
                classList.addClass(clazz);
            }
        }
    }

    private void addDependencies(VisualizationContext context) {
        for (JavaClass clazz : classList.getAllClassesOrderByDepth()) {
            for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                if (context.isElementIncluded(dependency.getTargetClass())
                        && !classList.containsClass(dependency.getTargetClass().getName())
                        && isClassValid(dependency.getTargetClass())) {
                    dependenciesClassList.addClassAndEnclosingClasses(dependency.getTargetClass());
                }
            }
        }
    }

    Iterable<JavaClass> getClasses() {
        return classList.getAllClassesOrderByDepth();
    }

    Iterable<JavaClass> getDependenciesClasses() {
        return dependenciesClassList.getAllClassesOrderByDepth();
    }

    Set<String> getPackages() {
        Set<String> result = new HashSet<>();
        for (JavaClass c : getAll()) {
            if (c.getPackage().isEmpty()) {
                throw new RuntimeException("A class with an empty package was found");
            }
            result.add(c.getPackage());
        }
        return result;
    }

    Iterable<JavaClass> getAll() {
        return Iterables.concat(getClasses(), getDependenciesClasses());
    }

    static ClassesToVisualize from(JavaClasses classes, VisualizationContext context) {
        return new ClassesToVisualize(classes, context);
    }

    /**
     * Stores classes grouped by their depth in the inner-class-hierarchy
     */
    private static class ClassList {
        private SortedMap<Integer, Map<String, JavaClass>> classes = new TreeMap<>();

        void addClass(JavaClass clazz) {
            int depth = getInnerClassDepth(clazz);
            Map<String, JavaClass> map = classes.containsKey(depth) ? classes.get(depth) : new HashMap<String, JavaClass>();
            map.put(clazz.getName(), clazz);
            classes.put(depth, map);
        }

        void addClassAndEnclosingClasses(JavaClass clazz) {
            addClass(clazz);
            if (clazz.getEnclosingClass().isPresent()) {
                addClassAndEnclosingClasses(clazz.getEnclosingClass().get());
            }
        }

        boolean containsClass(String fullName) {
            for (Map<String, JavaClass> map : classes.values()) {
                if (map.containsKey(fullName)) {
                    return true;
                }
            }
            return false;
        }

        Iterable<JavaClass> getAllClassesOrderByDepth() {
            Iterable<JavaClass> result = Collections.emptyList();
            for (Map<String, JavaClass> map : classes.values()) {
                result = Iterables.concat(result, map.values());
            }
            return result;
        }

        private int getInnerClassDepth(JavaClass clazz) {
            return !clazz.getEnclosingClass().isPresent() ? 0 : 1 + getInnerClassDepth(clazz.getEnclosingClass().get());
        }
    }
}
