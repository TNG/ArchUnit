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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private static final String INNER_CLASS_SEPARATOR = "$";

    void export(JavaClasses classes, File file, VisualizationContext context) {
        VisualizedClasses visualizedClasses = VisualizedClasses.from(classes, context);
        JsonJavaPackage root = createStructure(visualizedClasses, context);
        writeToFile(file, root);
    }

    private JsonJavaPackage createStructure(VisualizedClasses visualizedClasses, VisualizationContext context) {
        JsonJavaPackage root = PackageStructureCreator.createPackageStructure(visualizedClasses.getAll());
        insertVisualizedClassesToRoot(visualizedClasses, context, root);
        root.normalize();
        return root;
    }

    private void insertVisualizedClassesToRoot(VisualizedClasses visualizedClasses, VisualizationContext context, JsonJavaPackage root) {
        insertClassesToRoot(visualizedClasses.getClasses(), context, root);
        insertInnerClassesToRoot(visualizedClasses.getInnerClasses(), context, root);
        insertDependenciesToRoot(visualizedClasses.getDependencies(), root);
    }

    private void insertDependenciesToRoot(Iterable<JavaClass> dependencies, JsonJavaPackage root) {
        for (JavaClass c : dependencies) {
            root.insertJavaElement(parseJavaElementWithoutDependencies(c));
        }
    }

    private void insertInnerClassesToRoot(Iterable<JavaClass> innerClasses, VisualizationContext context, JsonJavaPackage root) {
        for (JavaClass c : innerClasses) {
            if (c.isAnonymous()) {
                addDependenciesOfAnonymousInnerClassToParent(context, root, c);
            } else {
                root.insertJavaElement(parseJavaElement(c, context));
            }
        }
    }

    private void insertClassesToRoot(Iterable<JavaClass> classes, VisualizationContext context, JsonJavaPackage root) {
        for (JavaClass c : classes) {
            root.insertJavaElement(parseJavaElement(c, context));
        }
    }

    private void writeToFile(File file, JsonJavaPackage root) {
        final GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(root, writer);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    private void addDependenciesOfAnonymousInnerClassToParent(VisualizationContext context, JsonJavaPackage root, JavaClass c) {
        Optional<? extends JsonElement> parent = root.getChild(c.getEnclosingClass().get().getName());
        if (parent.isPresent() && parent.get() instanceof JsonJavaElement) {
            JsonJavaElement el = (JsonJavaElement) parent.get();
            parseAnonymousImplementationToJavaElement(c, context, el);
            parseAccessesToJavaElement(c, context, el);
        }
    }

    private JsonJavaElement parseJavaElementWithoutDependencies(JavaClass clazz) {
        if (clazz.isInterface()) {
            return new JsonJavaInterface(clazz.getSimpleName(), getCleanedFullName(clazz.getName()));
        } else {
            return new JsonJavaClass(clazz.getSimpleName(), getCleanedFullName(clazz.getName()), "class", "");
        }
    }

    private JsonJavaElement parseJavaElement(JavaClass clazz, VisualizationContext context) {
        if (clazz.isInterface()) {
            return parseJavaInterface(clazz, context);
        } else {
            return parseJavaClass(clazz, context);
        }
    }

    private String getSuperClass(JavaClass clazz, VisualizationContext context) {
        if (!clazz.getSuperClass().isPresent()) {
            return "";
        }

        String superClassName = getCleanedFullName(clazz.getSuperClass().get().getName());
        return context.isElementIncluded(superClassName) ? superClassName : "";
    }

    // FIXME AU-18: ArchUnit shows fqn of inner classes with '$', so we should do this here as well, to be consistent
    static String getCleanedFullName(String fullName) {
        return fullName.replace(INNER_CLASS_SEPARATOR, PackageStructureCreator.PACKAGE_SEPARATOR);
    }

    private JsonJavaElement parseJavaClass(final JavaClass c, VisualizationContext context) {
        final JsonJavaClass res = new JsonJavaClass(c.getSimpleName(), getCleanedFullName(c.getName()),
                "class", getSuperClass(c, context));
        parseToJavaElement(c, context, res);
        return res;
    }

    private JsonJavaElement parseJavaInterface(JavaClass c, VisualizationContext context) {
        JsonJavaInterface res = new JsonJavaInterface(c.getSimpleName(), getCleanedFullName(c.getName()));
        parseToJavaElement(c, context, res);
        return res;
    }

    private void parseToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        parseImplementationToJavaElement(c, context, res);
        parseAccessesToJavaElement(c, context, res);
    }

    private void parseImplementationToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        for (JavaClass i : c.getAllInterfaces()) {
            if (context.isElementIncluded(i.getName())) {
                res.addInterface(getCleanedFullName(i.getName()));
            }
        }
    }

    private void parseAnonymousImplementationToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        for (JavaClass i : c.getAllInterfaces()) {
            if (context.isElementIncluded(i.getName())) {
                res.addAnonImpl(getCleanedFullName(i.getName()));
            }
        }
    }

    private void parseAccessesToJavaElement(JavaClass c, VisualizationContext context, JsonJavaElement res) {
        // FIXME: Don't use shortcuts like c, fa, etc., we're not writing Assembler ;-) The chars don't cost, rather make simpler if(..) expressions
        for (JavaFieldAccess fa : filterRelevantAccesses(context, c.getFieldAccessesFromSelf())) {
            res.addFieldAccess(new JsonAccess(fa));
        }
        for (JavaMethodCall mc : filterRelevantAccesses(context, c.getMethodCallsFromSelf())) {
            res.addMethodCall(new JsonAccess(mc));
        }
        for (JavaConstructorCall cc : filterRelevantAccesses(context, c.getConstructorCallsFromSelf())) {
            res.addConstructorCall(new JsonAccess(cc));
        }
    }

    private <T extends JavaAccess<?>> Set<T> filterRelevantAccesses(VisualizationContext context, Set<T> accesses) {
        Set<T> result = new HashSet<>();
        for (T access : accesses) {
            if (targetIsRelevant(access) && context.isElementIncluded(access.getTargetOwner())) {
                result.add(access);
            }
        }
        return result;
    }

    private <T extends JavaAccess<?>> boolean targetIsRelevant(T access) {
        return !access.getTargetOwner().isAnonymous() && !access.getOriginOwner().equals(access.getTargetOwner());
    }
}
