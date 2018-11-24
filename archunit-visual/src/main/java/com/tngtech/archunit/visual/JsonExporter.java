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

import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethodCall;

class JsonExporter {
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    String exportToJson(JavaClasses classes) {
        ClassesToVisualize classesToVisualize = ClassesToVisualize.from(classes);
        JsonJavaPackage root = createPackageClassTree(classesToVisualize);
        Set<JsonJavaDependency> dependencies = extractDependenciesFromClasses(classesToVisualize);
        return GSON.toJson(new JsonExport(root, dependencies));
    }

    private Set<JsonJavaDependency> extractDependenciesFromClasses(ClassesToVisualize classesToVisualize) {
        Set<JsonJavaDependency> result = new HashSet<>();
        for (JavaClass c : classesToVisualize.getClasses()) {
            result.addAll(extractDependenciesFromClass(c));
        }
        return result;
    }

    private Set<JsonJavaDependency> extractDependenciesFromClass(JavaClass javaClass) {
        Set<JsonJavaDependency> result = new HashSet<>();
        for (Dependency d : javaClass.getDirectDependenciesFromSelf()) {
            if (isDependencyRelevant(d)) {
                result.add(JsonJavaDependency.from(d));
            }
        }
        return result;
    }

    private JsonJavaPackage createPackageClassTree(ClassesToVisualize classesToVisualize) {
        JsonJavaPackage root = JsonJavaPackage.createPackageStructure(classesToVisualize.getPackages());
        insertVisualizedClassesToRoot(classesToVisualize, root);
        root.normalize();
        return root;
    }

    private void insertVisualizedClassesToRoot(ClassesToVisualize classesToVisualize, JsonJavaPackage root) {
        insertClassesToRoot(classesToVisualize.getClasses(), root);
        insertDependenciesToRoot(classesToVisualize.getDependenciesClasses(), root);
    }

    private void insertClassesToRoot(Iterable<JavaClass> classes, JsonJavaPackage root) {
        for (JavaClass c : classes) {
            root.insert(parseJavaElement(c));
        }
    }

    private void insertDependenciesToRoot(Iterable<JavaClass> dependencies, JsonJavaPackage root) {
        for (JavaClass c : dependencies) {
            root.insert(parseJavaElementWithoutDependencies(c));
        }
    }

    private JsonJavaElement parseJavaElementWithoutDependencies(JavaClass clazz) {
        if (clazz.isInterface()) {
            return new JsonJavaInterface(clazz);
        } else {
            return new JsonJavaClass(clazz, false);
        }
    }

    private JsonJavaElement parseJavaElement(JavaClass clazz) {
        if (clazz.isInterface()) {
            return parseJavaInterface(clazz);
        } else {
            return parseJavaClass(clazz);
        }
    }

    private JsonJavaElement parseJavaClass(final JavaClass javaClass) {
        final JsonJavaClass jsonJavaClass = new JsonJavaClass(javaClass, true);
        parseToJavaElement(javaClass, jsonJavaClass);
        return jsonJavaClass;
    }

    private JsonJavaElement parseJavaInterface(JavaClass javaClass) {
        JsonJavaInterface jsonJavaInterface = new JsonJavaInterface(javaClass);
        parseToJavaElement(javaClass, jsonJavaInterface);
        return jsonJavaInterface;
    }

    private void parseToJavaElement(JavaClass javaClass, JsonJavaElement result) {
        parseImplementationToJavaElement(javaClass, result);
        parseAccessesToJavaElement(javaClass, result);
    }

    private void parseImplementationToJavaElement(JavaClass javaClass, JsonJavaElement res) {
        for (JavaClass iFace : javaClass.getInterfaces()) {
            res.addInterface(iFace.getName());
        }
    }

    private void parseAccessesToJavaElement(JavaClass javaClass, JsonJavaElement jsonJavaElement) {
        for (JavaFieldAccess javaFieldAccess : filterRelevantAccesses(javaClass.getFieldAccessesFromSelf(), jsonJavaElement)) {
            jsonJavaElement.addFieldAccess(new JsonAccess(javaFieldAccess));
        }
        for (JavaMethodCall javaMethodCall : filterRelevantAccesses(javaClass.getMethodCallsFromSelf(), jsonJavaElement)) {
            jsonJavaElement.addMethodCall(new JsonAccess(javaMethodCall));
        }
        for (JavaConstructorCall javaConstructorCall : filterRelevantAccesses(javaClass.getConstructorCallsFromSelf(), jsonJavaElement)) {
            jsonJavaElement.addConstructorCall(new JsonAccess(javaConstructorCall));
        }
    }

    private <T extends JavaAccess<?>> Set<T> filterRelevantAccesses(Set<T> accesses, JsonJavaElement jsonJavaElement) {
        Set<T> result = new HashSet<>();
        for (T access : accesses) {
            if (targetIsRelevant(access, jsonJavaElement)) {
                result.add(access);
            }
        }
        return result;
    }

    private <T extends JavaAccess<?>> boolean targetIsRelevant(T access, JsonJavaElement jsonJavaElement) {
        return !access.getTargetOwner().isAnonymous() && !access.getOriginOwner().equals(access.getTargetOwner())
                && !jsonJavaElement.fullName.equals(access.getTargetOwner().getName()) && !access.getTargetOwner().getPackageName().isEmpty();
    }

    private boolean isDependencyRelevant(Dependency d) {
        return !d.getTargetClass().isEquivalentTo(Object.class)
                && !d.getTargetClass().isAnonymous()
                && !d.getTargetClass().getPackageName().isEmpty();
    }
}
