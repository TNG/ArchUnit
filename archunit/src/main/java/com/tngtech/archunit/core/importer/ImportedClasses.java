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
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassBuilder;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.importer.ImportedClasses.ImportedClassState.HAD_TO_BE_IMPORTED;
import static com.tngtech.archunit.core.importer.ImportedClasses.ImportedClassState.WAS_ALREADY_PRESENT;

class ImportedClasses {
    private static final ImmutableSet<JavaModifier> PRIMITIVE_AND_ARRAY_TYPE_MODIFIERS =
            Sets.immutableEnumSet(PUBLIC, ABSTRACT, FINAL);

    private final ImmutableMap<String, JavaClass> directlyImported;
    private final Map<String, JavaClass> allClasses = new HashMap<>();
    private final ClassResolver resolver;
    private final MethodReturnTypeGetter getMethodReturnType;

    ImportedClasses(Map<String, JavaClass> directlyImported, ClassResolver resolver, MethodReturnTypeGetter methodReturnTypeGetter) {
        this.directlyImported = ImmutableMap.copyOf(directlyImported);
        allClasses.putAll(directlyImported);
        this.resolver = resolver;
        this.getMethodReturnType = methodReturnTypeGetter;
    }

    Map<String, JavaClass> getDirectlyImported() {
        return directlyImported;
    }

    JavaClass getOrResolve(String typeName) {
        JavaClass javaClass = allClasses.get(typeName);
        return javaClass != null ? javaClass : resolve(typeName);
    }

    ImportedClassState ensurePresent(String typeName) {
        if (allClasses.containsKey(typeName)) {
            return WAS_ALREADY_PRESENT;
        }

        resolve(typeName);
        return HAD_TO_BE_IMPORTED;
    }

    private JavaClass resolve(String typeName) {
        Optional<JavaClass> resolved = resolver.tryResolve(typeName);
        JavaClass javaClass = resolved.isPresent() ? resolved.get() : stubClassOf(typeName);
        if (javaClass.isArray()) {
            ensureAllComponentTypesPresent(javaClass);
        }
        allClasses.put(typeName, javaClass);
        return javaClass;
    }

    private void ensureAllComponentTypesPresent(JavaClass javaClass) {
        JavaClassDescriptor current = JavaClassDescriptor.From.javaClass(javaClass);
        while (current.tryGetComponentType().isPresent()) {
            current = current.tryGetComponentType().get();
            ensurePresent(current.getFullyQualifiedClassName());
        }
    }

    Collection<JavaClass> getAllWithOuterClassesSortedBeforeInnerClasses() {
        return ImmutableSortedMap.copyOf(allClasses).values();
    }

    private static JavaClass stubClassOf(String typeName) {
        JavaClassDescriptor descriptor = JavaClassDescriptor.From.name(typeName);
        JavaClassBuilder builder = JavaClassBuilder.forStub().withDescriptor(descriptor);
        addModifiersIfPossible(builder, descriptor);
        return builder.build();
    }

    private static void addModifiersIfPossible(JavaClassBuilder builder, JavaClassDescriptor descriptor) {
        if (descriptor.isPrimitive() || descriptor.isArray()) {
            builder.withModifiers(PRIMITIVE_AND_ARRAY_TYPE_MODIFIERS);
        }
    }

    public Optional<JavaClass> getMethodReturnType(String declaringClassName, String methodName) {
        return getMethodReturnType.getReturnType(declaringClassName, methodName);
    }

    interface MethodReturnTypeGetter {
        Optional<JavaClass> getReturnType(String declaringClassName, String methodName);
    }

    enum ImportedClassState {
        HAD_TO_BE_IMPORTED,
        WAS_ALREADY_PRESENT
    }
}
