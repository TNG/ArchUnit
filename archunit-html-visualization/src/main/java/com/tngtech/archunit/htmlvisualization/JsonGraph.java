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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.google.common.collect.Iterables.getOnlyElement;

class JsonGraph {
    private static final String JSON_DEFAULT_PACKAGE_NAME = "default";

    private final JsonJavaPackage root;

    private JsonGraph(JavaClasses classes) {
        root = new JsonJavaPackage(classes.getDefaultPackage());
        root.addClasses(getRelevantClasses(classes));
    }

    private static Set<JavaClass> getRelevantClasses(JavaClasses classes) {
        Set<JavaClass> result = new HashSet<>();
        for (JavaClass javaClass : classes) {
            if (isRelevant(javaClass)) {
                result.add(javaClass.getBaseComponentType());
            }
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                if (isRelevant(dependency.getTargetClass())) {
                    result.add(dependency.getTargetClass().getBaseComponentType());
                }
            }
        }
        return result;
    }

    private static boolean isRelevant(JavaClass javaClass) {
        return !javaClass.getPackageName().isEmpty();
    }

    public Map<String, JsonSerializable> toJsonSerializable() {
        return ImmutableMap.of("root", defaultPackageToJsonSerializable());
    }

    private JsonSerializable defaultPackageToJsonSerializable() {
        Set<JsonSerializable> children = new HashSet<>();
        for (JsonJavaPackage jsonJavaPackage : root.getSubpackages()) {
            children.add(flatten(jsonJavaPackage.toJsonSerializable()));
        }
        return new JsonSerializable(JSON_DEFAULT_PACKAGE_NAME, JSON_DEFAULT_PACKAGE_NAME, JsonJavaPackage.TYPE, children);
    }

    private JsonSerializable flatten(JsonSerializable serializablePackage) {
        JsonSerializable flattenedPackage = serializablePackage;
        while (hasNoClassesInsideAndOnlyOneSubpackage(flattenedPackage)) {
            flattenedPackage = getOnlyElement(flattenedPackage.children);
        }
        return flattenedPackage.withName(flattenedPackage.fullName);
    }

    private boolean hasNoClassesInsideAndOnlyOneSubpackage(JsonSerializable flattenedPackage) {
        return flattenedPackage.type.equals(JsonJavaPackage.TYPE)
                && flattenedPackage.children.size() == 1
                && getOnlyElement(flattenedPackage.children).type.equals(JsonJavaPackage.TYPE);
    }

    public static JsonGraph from(JavaClasses classes) {
        return new JsonGraph(classes);
    }
}
