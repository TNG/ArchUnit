/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassBuilder;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;

import static com.google.common.collect.Sets.immutableEnumSet;
import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.PROTECTED;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.importer.ImportedClasses.ImportedClassState.HAD_TO_BE_IMPORTED;
import static com.tngtech.archunit.core.importer.ImportedClasses.ImportedClassState.WAS_ALREADY_PRESENT;

class ImportedClasses {
    private static final ImmutableSet<JavaModifier> PRIMITIVE_TYPE_MODIFIERS =
            immutableEnumSet(PUBLIC, ABSTRACT, FINAL);
    private static final ImmutableSet<JavaModifier> VISIBILITY_MODIFIERS =
            immutableEnumSet(PUBLIC, PROTECTED, PRIVATE);

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

    private JavaClass stubClassOf(String typeName) {
        JavaClassDescriptor descriptor = JavaClassDescriptor.From.name(typeName);
        JavaClassBuilder builder = JavaClassBuilder.forStub().withDescriptor(descriptor);
        addModifiersIfPossible(builder, descriptor);
        return builder.build();
    }

    /**
     * See {@link Class#getModifiers()}
     */
    private void addModifiersIfPossible(JavaClassBuilder builder, JavaClassDescriptor descriptor) {
        if (descriptor.isPrimitive()) {
            builder.withModifiers(PRIMITIVE_TYPE_MODIFIERS);
        } else if (descriptor.isArray()) {
            JavaClass elementType = getOrResolve(getElementType(descriptor).getFullyQualifiedClassName());
            Set<JavaModifier> modifiers = ImmutableSet.<JavaModifier>builder()
                    .addAll(getVisibility(elementType))
                    .add(ABSTRACT, FINAL)
                    .build();
            builder.withModifiers(modifiers);
        }
    }

    private JavaClassDescriptor getElementType(JavaClassDescriptor descriptor) {
        return descriptor.tryGetComponentType().map(this::getElementType).orElse(descriptor);
    }

    private Set<JavaModifier> getVisibility(JavaClass javaClass) {
        return Sets.intersection(VISIBILITY_MODIFIERS, javaClass.getModifiers());
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
