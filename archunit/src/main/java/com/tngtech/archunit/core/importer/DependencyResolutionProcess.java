/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.importer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;

class DependencyResolutionProcess {
    private final Set<String> typeNames = new HashSet<>();
    private boolean initializationComplete = false;

    void registerMemberType(String typeName) {
        if (!initializationComplete) {
            typeNames.add(typeName);
        }
    }

    void registerMemberTypes(Collection<String> typeNames) {
        for (String typeName : typeNames) {
            registerMemberType(typeName);
        }
    }

    void registerAccessToType(String typeName) {
        if (!initializationComplete) {
            typeNames.add(typeName);
        }
    }

    void registerSupertype(String typeName) {
        if (!initializationComplete) {
            typeNames.add(typeName);
        }
    }

    void registerSupertypes(Collection<String> typeNames) {
        for (String typeName : typeNames) {
            registerSupertype(typeName);
        }
    }

    void resolve(ImportedClasses classes, ClassFileImportRecord importRecord) {
        initializationComplete = true;
        for (String typeName : typeNames) {
            classes.ensurePresent(typeName);
            resolveInheritance(typeName, classes, importRecord);
        }
        ensureMetaAnnotationsArePresent(classes, importRecord);
    }

    private void resolveInheritance(String typeName, ImportedClasses classes, ClassFileImportRecord importRecord) {
        Optional<String> superclass = importRecord.getSuperclassFor(typeName);
        if (superclass.isPresent()) {
            classes.ensurePresent(superclass.get());
            resolveInheritance(superclass.get(), classes, importRecord);
        }
        for (String interfaceName : importRecord.getInterfaceNamesFor(typeName)) {
            classes.ensurePresent(interfaceName);
            resolveInheritance(interfaceName, classes, importRecord);
        }
    }

    private void ensureMetaAnnotationsArePresent(ImportedClasses classes, ClassFileImportRecord importRecord) {
        for (JavaClass javaClass : classes.getAllWithOuterClassesSortedBeforeInnerClasses()) {
            resolveAnnotationHierarchy(javaClass, classes, importRecord);
        }
    }

    private void resolveAnnotationHierarchy(JavaClass javaClass, ImportedClasses classes, ClassFileImportRecord importRecord) {
        for (String annotationTypeName : getAnnotationTypeNamesToResolveFor(javaClass, importRecord)) {
            boolean hadBeenPreviouslyResolved = classes.isPresent(annotationTypeName);
            JavaClass annotationType = classes.getOrResolve(annotationTypeName);

            if (!hadBeenPreviouslyResolved) {
                resolveAnnotationHierarchy(annotationType, classes, importRecord);
            }
        }
    }

    private Set<String> getAnnotationTypeNamesToResolveFor(JavaClass javaClass, ClassFileImportRecord importRecord) {
        return ImmutableSet.<String>builder()
                .addAll(importRecord.getAnnotationTypeNamesFor(javaClass))
                .addAll(importRecord.getMemberAnnotationTypeNamesFor(javaClass))
                .addAll(importRecord.getParameterAnnotationTypeNamesFor(javaClass))
                .build();
    }
}
