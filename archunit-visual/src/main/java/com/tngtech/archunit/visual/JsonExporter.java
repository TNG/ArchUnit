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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tngtech.archunit.core.domain.*;

import java.util.HashSet;
import java.util.Set;

class JsonExporter {
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    String exportToJson(JavaClasses classes, VisualizationContext context) {
        ClassesToVisualize classesToVisualize = ClassesToVisualize.from(classes, context);
        JsonJavaPackage root = createPackageClassTree(classesToVisualize, context);
        Set<JsonJavaDependency> dependencies = extractDependenciesFromClasses(classesToVisualize, context, root);
        return GSON.toJson(new JsonExport(root, dependencies));
    }

    private Set<JsonJavaDependency> extractDependenciesFromClasses(ClassesToVisualize classesToVisualize, VisualizationContext context, JsonJavaPackage root) {
        Set<JsonJavaDependency> result = new HashSet<>();
        for (JavaClass c : classesToVisualize.getClasses()) {
            result.addAll(extractDependenciesFromClass(c, context, root));
        }
        return result;
    }

    private Set<JsonJavaDependency> extractDependenciesFromClass(JavaClass javaClass, VisualizationContext context, JsonJavaPackage root) {
        Set<JsonJavaDependency> res = new HashSet<>();
        for (Dependency d : javaClass.getDirectDependenciesFromSelf()) {
            if (context.isElementIncluded(d.getTargetClass()) && isDependencyRelevant(d)) {
                res.add(JsonJavaDependency.from(d));
            }
        }
        return res;
    }

    private JsonJavaPackage createPackageClassTree(ClassesToVisualize classesToVisualize, VisualizationContext context) {
        JsonJavaPackage root = JsonJavaPackage.createPackageStructure(classesToVisualize.getPackages());
        insertVisualizedClassesToRoot(classesToVisualize, context, root);
        root.normalize();
        return root;
    }

    private void insertVisualizedClassesToRoot(ClassesToVisualize classesToVisualize, VisualizationContext context, JsonJavaPackage root) {
        insertClassesToRoot(classesToVisualize.getClasses(), context, root);
        insertDependenciesToRoot(classesToVisualize.getDependenciesClasses(), root);
    }

    private void insertClassesToRoot(Iterable<JavaClass> classes, VisualizationContext context, JsonJavaPackage root) {
        for (JavaClass c : classes) {
            root.insert(parseJavaElement(c, context));
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

    private JsonJavaElement parseJavaElement(JavaClass clazz, VisualizationContext context) {
        if (clazz.isInterface()) {
            return parseJavaInterface(clazz, context);
        } else {
            return parseJavaClass(clazz, context);
        }
    }

    private JsonJavaElement parseJavaClass(final JavaClass javaClass, VisualizationContext context) {
        final JsonJavaClass jsonJavaClass = new JsonJavaClass(javaClass, context.isElementIncluded(javaClass.getSuperClass()));
        parseToJavaElement(javaClass, context, jsonJavaClass);
        return jsonJavaClass;
    }

    private JsonJavaElement parseJavaInterface(JavaClass javaClass, VisualizationContext context) {
        JsonJavaInterface jsonJavaInterface = new JsonJavaInterface(javaClass);
        parseToJavaElement(javaClass, context, jsonJavaInterface);
        return jsonJavaInterface;
    }

    private void parseToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        parseImplementationToJavaElement(c, context, res);
        parseAccessesToJavaElement(c, context, res);
    }

    private void parseImplementationToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        for (JavaClass javaClass : c.getInterfaces()) {
            if (context.isElementIncluded(javaClass)) {
                res.addInterface(javaClass.getName());
            }
        }
    }

    private void parseAccessesToJavaElement(JavaClass javaClass, VisualizationContext context, JsonJavaElement jsonJavaElement) {
        for (JavaFieldAccess javaFieldAccess : filterRelevantAccesses(context, javaClass.getFieldAccessesFromSelf(), jsonJavaElement)) {
            jsonJavaElement.addFieldAccess(new JsonAccess(javaFieldAccess));
        }
        for (JavaMethodCall javaMethodCall : filterRelevantAccesses(context, javaClass.getMethodCallsFromSelf(), jsonJavaElement)) {
            jsonJavaElement.addMethodCall(new JsonAccess(javaMethodCall));
        }
        for (JavaConstructorCall javaConstructorCall : filterRelevantAccesses(context, javaClass.getConstructorCallsFromSelf(), jsonJavaElement)) {
            jsonJavaElement.addConstructorCall(new JsonAccess(javaConstructorCall));
        }
    }

    private <T extends JavaAccess<?>> Set<T> filterRelevantAccesses(VisualizationContext context, Set<T> accesses, JsonJavaElement jsonJavaElement) {
        Set<T> result = new HashSet<>();
        for (T access : accesses) {
            if (targetIsRelevant(access, jsonJavaElement) && context.isElementIncluded(access.getTargetOwner())) {
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
        return !d.getTargetClass().isAnonymous() && !d.getTargetClass().equals(d.getOriginClass())
                && !d.getTargetClass().getName().equals(d.getOriginClass().getName())
                && !d.getTargetClass().getPackageName().isEmpty();
    }
}
