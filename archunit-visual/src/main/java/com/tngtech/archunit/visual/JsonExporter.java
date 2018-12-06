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
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

import java.util.HashSet;
import java.util.Set;

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
        insertClassesToRoot(classesToVisualize.getDependenciesClasses(), root);
    }

    private void insertClassesToRoot(Iterable<JavaClass> classes, JsonJavaPackage root) {
        for (JavaClass c : classes) {
            root.insert(parseJavaElement(c));
        }
    }

    private JsonJavaElement parseJavaElement(JavaClass clazz) {
        if (clazz.isInterface()) {
            return new JsonJavaInterface(clazz);
        } else {
            return new JsonJavaClass(clazz);
        }
    }

    private boolean isDependencyRelevant(Dependency d) {
        return !d.getTargetClass().isEquivalentTo(Object.class) && !isDefaultDependencyFromInnerClassToEnclosingClass(d) && !isDependencyToPrimitiveArray(d);
    }

    private boolean isDefaultDependencyFromInnerClassToEnclosingClass(Dependency d) {
        boolean isDependencyFromInnerClassToEnclosingClass = d.getOriginClass().isInnerClass()
                && d.getOriginClass().getEnclosingClass().isPresent() //should be unnecessary - but is safer
                && d.getOriginClass().getEnclosingClass().get().getName().equals(d.getTargetClass().getName());
        boolean isDefaultDependency = d.getLineNumber() == 0 &&
                (d.getType() == Dependency.Type.CONSTRUCTOR_PARAMETER_TYPE || d.getType() == Dependency.Type.FIELD_TYPE);
        return isDependencyFromInnerClassToEnclosingClass && isDefaultDependency;
    }

    private boolean isDependencyToPrimitiveArray(Dependency d) {
        return d.getTargetClass().getName().startsWith(JsonJavaDependency.ARRAY_MARKER)
                && !d.getTargetClass().getName().startsWith(JsonJavaDependency.ARRAY_MARKER + JsonJavaDependency.OBJECT_MARKER);
    }
}
