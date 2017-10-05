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

import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethodCall;

class JsonExporter {

    private Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    void export(JavaClasses classes, Writer writer, VisualizationContext context) {
        ClassesToVisualize classesToVisualize = ClassesToVisualize.from(classes, context);
        JsonJavaPackage root = createPackageClassTree(classesToVisualize, context);
        writeToWriter(root, writer);
    }

    private JsonJavaPackage createPackageClassTree(ClassesToVisualize classesToVisualize, VisualizationContext context) {
        JsonJavaPackage root = JsonJavaPackage.createPackageStructure(classesToVisualize.getPackages());
        insertVisualizedClassesToRoot(classesToVisualize, context, root);
        root.normalize();
        return root;
    }

    private void insertVisualizedClassesToRoot(ClassesToVisualize classesToVisualize, VisualizationContext context, JsonJavaPackage root) {
        insertClassesToRoot(classesToVisualize.getClasses(), context, root);
        insertInnerClassesToRoot(classesToVisualize.getInnerClasses(), context, root);
        insertDependenciesToRoot(classesToVisualize.getDependencies(), root);
    }

    private void insertClassesToRoot(Iterable<JavaClass> classes, VisualizationContext context, JsonJavaPackage root) {
        for (JavaClass c : classes) {
            root.insert(parseJavaElement(c, context));
        }
    }

    private void insertInnerClassesToRoot(Iterable<JavaClass> innerClasses, VisualizationContext context, JsonJavaPackage root) {
        for (JavaClass c : innerClasses) {
            if (c.isAnonymous()) {
                addDependenciesOfAnonymousInnerClassToParent(context, root, c);
            } else {
                root.insert(parseJavaElement(c, context));
            }
        }
    }

    private void insertDependenciesToRoot(Iterable<JavaClass> dependencies, JsonJavaPackage root) {
        for (JavaClass c : dependencies) {
            root.insert(parseJavaElementWithoutDependencies(c));
        }
    }

    private void writeToWriter(JsonJavaPackage root, Writer writer) {
        gson.toJson(root, writer);
    }

    private void addDependenciesOfAnonymousInnerClassToParent(VisualizationContext context, JsonJavaPackage root, JavaClass anonymousInnerClass) {
        Optional<? extends JsonElement> optionalParent = root.getChild(anonymousInnerClass.getEnclosingClass().get().getName());
        if (optionalParent.isPresent() && optionalParent.get() instanceof JsonJavaElement) {
            JsonJavaElement parent = (JsonJavaElement) optionalParent.get();
            parseAnonymousImplementationToJavaElement(anonymousInnerClass, context, parent);
            parseAccessesToJavaElement(anonymousInnerClass, context, parent);
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
        for (JavaClass javaClass : c.getAllInterfaces()) {
            if (context.isElementIncluded(javaClass)) {
                res.addInterface(javaClass.getName());
            }
        }
    }

    private void parseAnonymousImplementationToJavaElement(JavaClass clazz, VisualizationContext context, JsonJavaElement res) {
        for (JavaClass anInterface : clazz.getAllInterfaces()) {
            if (context.isElementIncluded(anInterface)) {
                res.addAnonymousImplementation(anInterface.getName());
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
                && !jsonJavaElement.fullName.equals(access.getTargetOwner().getName());
    }
}
