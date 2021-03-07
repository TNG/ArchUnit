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
package com.tngtech.archunit.htmlvisualization;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

class JsonJavaPackage implements ArchJsonElement {
    static final String TYPE = "package";

    private final JavaPackage javaPackage;
    private final Map<String, JsonJavaPackage> subpackages = new TreeMap<>();
    private final Map<String, JsonJavaClass> classes = new TreeMap<>();

    JsonJavaPackage(JavaPackage javaPackage) {
        this.javaPackage = javaPackage;
        addSubpackages(javaPackage.getSubpackages());
    }

    Set<JsonJavaPackage> getSubpackages() {
        return ImmutableSet.copyOf(subpackages.values());
    }

    void addClasses(Set<JavaClass> classes) {
        for (JavaClass javaClass : FluentIterable.from(classes).toSortedSet(OUTER_BEFORE_INNER)) {
            addClass(javaClass);
        }
    }

    private void addClass(JavaClass javaClass) {
        LinkedList<JavaPackage> relativePackagePath = new LinkedList<>(singleton(javaClass.getPackage()));
        // we can't compare equals, because dependency targets might just have a stub package
        while (!relativePackagePath.get(0).getName().equals(javaPackage.getName())) {
            relativePackagePath.add(0, relativePackagePath.get(0).getParent().get());
        }
        addClass(relativePackagePath.subList(1, relativePackagePath.size()), javaClass);
    }

    private void addClass(List<JavaPackage> relativePackagePath, JavaClass javaClass) {
        if (!relativePackagePath.isEmpty()) {
            JsonJavaPackage jsonJavaPackage = getOrCreateSubpackage(relativePackagePath.get(0));
            jsonJavaPackage.addClass(relativePackagePath.subList(1, relativePackagePath.size()), javaClass);
            return;
        }

        JavaClass baseComponentType = javaClass.getBaseComponentType();
        if (baseComponentType.getEnclosingClass().isPresent()) {
            addNestedClass(baseComponentType, javaClass);
        } else {
            classes.put(javaClass.getName(), JsonJavaClass.of(javaClass));
        }
    }

    private void addNestedClass(JavaClass baseComponentType, JavaClass javaClass) {
        LinkedList<JavaClass> nestedClassPath = new LinkedList<>(singleton(baseComponentType.getEnclosingClass().get()));
        while (nestedClassPath.get(0).getEnclosingClass().isPresent()) {
            nestedClassPath.add(0, nestedClassPath.get(0).getEnclosingClass().get());
        }
        JavaClass enclosingRootClass = requireNonNull(nestedClassPath.pollFirst());
        getOrAddClass(enclosingRootClass.getName()).addClass(nestedClassPath, javaClass);
    }

    private JsonJavaClass getOrAddClass(String fullyQualifiedClassName) {
        JsonJavaClass jsonJavaClass = classes.get(fullyQualifiedClassName);
        if (jsonJavaClass == null) {
            jsonJavaClass = new JsonJavaClass(
                    fullyQualifiedClassName.replaceAll(".*[.$]", ""),
                    fullyQualifiedClassName,
                    JsonJavaClass.CLASS_TYPE
            );
            classes.put(fullyQualifiedClassName, jsonJavaClass);
        }
        return jsonJavaClass;
    }

    private JsonJavaPackage getOrCreateSubpackage(JavaPackage javaPackage) {
        JsonJavaPackage result = subpackages.get(javaPackage.getRelativeName());
        if (result == null) {
            result = new JsonJavaPackage(javaPackage);
            subpackages.put(javaPackage.getRelativeName(), result);
        }
        return result;
    }

    private void addSubpackages(Set<JavaPackage> subpackages) {
        for (JavaPackage subpackage : subpackages) {
            this.subpackages.put(subpackage.getRelativeName(), new JsonJavaPackage(subpackage));
        }
    }

    @Override
    public JsonSerializable toJsonSerializable() {
        Set<JsonSerializable> childSerializables = new HashSet<>();
        for (ArchJsonElement child : Iterables.concat(subpackages.values(), classes.values())) {
            childSerializables.add(child.toJsonSerializable());
        }
        return new JsonSerializable(javaPackage.getRelativeName(), javaPackage.getName(), TYPE, childSerializables);
    }

    private static final Comparator<JavaClass> OUTER_BEFORE_INNER = new Ordering<JavaClass>() {
        @Override
        @SuppressWarnings("ConstantConditions") // we don't use null elements
        public int compare(JavaClass first, JavaClass second) {
            return ComparisonChain.start()
                    .compareFalseFirst(first.isArray(), second.isArray())
                    .compare(first.getName(), second.getName())
                    .result();
        }
    };
}
