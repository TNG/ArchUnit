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
package com.tngtech.archunit.core.domain;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaAnnotation.DefaultParameterVisitor;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.tngtech.archunit.base.Suppliers.memoize;

class JavaClassDependencies {
    private final JavaClass javaClass;
    private final Supplier<Set<Dependency>> directDependenciesFromClass;

    JavaClassDependencies(JavaClass javaClass) {
        this.javaClass = javaClass;
        this.directDependenciesFromClass = createDirectDependenciesFromClassSupplier();
    }

    private Supplier<Set<Dependency>> createDirectDependenciesFromClassSupplier() {
        return memoize(() ->
                Streams.concat(
                        dependenciesFromAccesses(javaClass.getAccessesFromSelf()),
                        inheritanceDependenciesFromSelf(),
                        fieldDependenciesFromSelf(),
                        returnTypeDependenciesFromSelf(),
                        codeUnitParameterDependenciesFromSelf(),
                        throwsDeclarationDependenciesFromSelf(),
                        tryCatchBlockDependenciesFromSelf(),
                        annotationDependenciesFromSelf(),
                        instanceofCheckDependenciesFromSelf(),
                        referencedClassObjectDependenciesFromSelf(),
                        typeParameterDependenciesFromSelf()
                ).collect(toImmutableSet())
        );
    }

    Set<Dependency> getDirectDependenciesFromClass() {
        return directDependenciesFromClass.get();
    }

    private Stream<Dependency> dependenciesFromAccesses(Set<JavaAccess<?>> accesses) {
        return accesses.stream().flatMap(access -> Dependency.tryCreateFromAccess(access).stream());
    }

    private Stream<Dependency> inheritanceDependenciesFromSelf() {
        Stream<Dependency> rawInheritanceDependencies = Stream.concat(
                javaClass.getRawInterfaces().stream(),
                javaClass.getRawSuperclass().map(Stream::of).orElse(Stream.empty())
        ).map(supertype -> Dependency.fromInheritance(javaClass, supertype));

        Stream<Dependency> genericInheritanceDependencies = Stream.concat(
                genericSuperclassTypeArgumentDependencies(),
                genericInterfaceTypeArgumentDependencies()
        );

        return Stream.concat(rawInheritanceDependencies, genericInheritanceDependencies);
    }

    private Stream<Dependency> genericSuperclassTypeArgumentDependencies() {
        if (!javaClass.getSuperclass().isPresent() || !(javaClass.getSuperclass().get() instanceof JavaParameterizedType)) {
            return Stream.empty();
        }
        JavaParameterizedType genericSuperclass = (JavaParameterizedType) javaClass.getSuperclass().get();

        return dependenciesOfTypes(genericSuperclass.getActualTypeArguments())
                .flatMap(superclassTypeArgumentDependency ->
                        Dependency.tryCreateFromGenericSuperclassTypeArguments(javaClass, genericSuperclass, superclassTypeArgumentDependency).stream());
    }

    private Stream<Dependency> genericInterfaceTypeArgumentDependencies() {
        return getGenericInterfacesOf(javaClass)
                .flatMap(genericInterface ->
                        dependenciesOfTypes(genericInterface.getActualTypeArguments())
                                .flatMap(interfaceTypeArgumentDependency ->
                                        Dependency.tryCreateFromGenericInterfaceTypeArgument(javaClass, genericInterface, interfaceTypeArgumentDependency).stream()));
    }

    private static Stream<JavaParameterizedType> getGenericInterfacesOf(JavaClass javaClass) {
        return javaClass.getInterfaces().stream()
                .filter(instanceOf(JavaParameterizedType.class))
                .map(type -> (JavaParameterizedType) type);
    }

    private Stream<Dependency> fieldDependenciesFromSelf() {
        return javaClass.getFields().stream()
                .flatMap(field -> Stream.concat(
                        Dependency.tryCreateFromField(field).stream(),
                        genericFieldTypeArgumentDependencies(field)
                ));
    }

    private Stream<Dependency> genericFieldTypeArgumentDependencies(JavaField field) {
        if (!(field.getType() instanceof JavaParameterizedType)) {
            return Stream.empty();
        }
        JavaParameterizedType fieldType = (JavaParameterizedType) field.getType();

        return dependenciesOfTypes(fieldType.getActualTypeArguments())
                .flatMap(fieldTypeArgumentDependency ->
                        Dependency.tryCreateFromGenericFieldTypeArgument(field, fieldTypeArgumentDependency).stream());
    }

    private Stream<Dependency> returnTypeDependenciesFromSelf() {
        return javaClass.getMethods().stream()
                .flatMap(method -> Stream.concat(
                        Dependency.tryCreateFromReturnType(method).stream(),
                        genericReturnTypeArgumentDependencies(method)
                ));
    }

    private Stream<Dependency> genericReturnTypeArgumentDependencies(JavaMethod method) {
        if (!(method.getReturnType() instanceof JavaParameterizedType)) {
            return Stream.empty();
        }
        JavaParameterizedType returnType = (JavaParameterizedType) method.getReturnType();

        return dependenciesOfTypes(returnType.getActualTypeArguments())
                .flatMap(returnTypeArgumentDependency ->
                        Dependency.tryCreateFromGenericMethodReturnTypeArgument(method, returnTypeArgumentDependency).stream());
    }

    private Stream<Dependency> codeUnitParameterDependenciesFromSelf() {
        return javaClass.getCodeUnits().stream()
                .flatMap(codeUnit -> Stream.concat(
                        rawParameterTypeDependencies(codeUnit),
                        genericParameterTypeArgumentDependencies(codeUnit)
                ));
    }

    private Stream<Dependency> rawParameterTypeDependencies(JavaCodeUnit codeUnit) {
        return codeUnit.getRawParameterTypes().stream()
                .flatMap(parameter -> Dependency.tryCreateFromParameter(codeUnit, parameter).stream());
    }

    private Stream<Dependency> genericParameterTypeArgumentDependencies(JavaCodeUnit codeUnit) {
        return codeUnit.getParameterTypes().stream()
                .filter(parameterType -> parameterType instanceof JavaParameterizedType)
                .flatMap(parameterType -> dependenciesOfParameterizedType((JavaParameterizedType) parameterType)
                        .flatMap(parameterTypeDependency ->
                                Dependency.tryCreateFromGenericCodeUnitParameterTypeArgument(codeUnit, parameterType, parameterTypeDependency).stream()));
    }

    private Stream<Dependency> throwsDeclarationDependenciesFromSelf() {
        return javaClass.getThrowsDeclarations().stream()
                .flatMap(throwsDeclaration -> Dependency.tryCreateFromThrowsDeclaration(throwsDeclaration).stream());
    }

    private Stream<Dependency> tryCatchBlockDependenciesFromSelf() {
        return javaClass.getTryCatchBlocks().stream()
                .flatMap(tryCatchBlock -> Dependency.tryCreateFromTryCatchBlock(tryCatchBlock).stream());
    }

    private Stream<Dependency> annotationDependenciesFromSelf() {
        return Streams.concat(
                annotationDependencies(javaClass),
                annotationDependencies(javaClass.getFields()),
                annotationDependencies(javaClass.getMethods()),
                parameterAnnotationDependencies(javaClass.getMethods()),
                annotationDependencies(javaClass.getConstructors()),
                parameterAnnotationDependencies(javaClass.getConstructors())
        );
    }

    private Stream<Dependency> instanceofCheckDependenciesFromSelf() {
        return javaClass.getInstanceofChecks().stream()
                .flatMap(instanceofCheck -> Dependency.tryCreateFromInstanceofCheck(instanceofCheck).stream());
    }

    private Stream<Dependency> referencedClassObjectDependenciesFromSelf() {
        return javaClass.getReferencedClassObjects().stream()
                .flatMap(referencedClassObject -> Dependency.tryCreateFromReferencedClassObject(referencedClassObject).stream());
    }

    private Stream<Dependency> typeParameterDependenciesFromSelf() {
        return Stream.concat(
                classTypeParameterDependenciesFromSelf(),
                codeUnitTypeParameterDependenciesFromSelf()
        );
    }

    private Stream<Dependency> classTypeParameterDependenciesFromSelf() {
        return javaClass.getTypeParameters().stream()
                .flatMap(this::getDependenciesFromTypeParameter);
    }

    private Stream<Dependency> codeUnitTypeParameterDependenciesFromSelf() {
        return javaClass.getCodeUnits().stream()
                .flatMap(codeUnit -> codeUnit.getTypeParameters().stream())
                .flatMap(this::getDependenciesFromTypeParameter);
    }

    private Stream<Dependency> getDependenciesFromTypeParameter(JavaTypeVariable<?> typeVariable) {
        return dependenciesOfTypes(typeVariable.getUpperBounds())
                .flatMap(typeParameterDependency -> Dependency.tryCreateFromTypeParameter(typeVariable, typeParameterDependency).stream());
    }

    private Stream<JavaClass> dependenciesOfTypes(Collection<JavaType> types) {
        return types.stream().flatMap(JavaClassDependencies::dependenciesOfType);
    }

    private static Stream<JavaClass> dependenciesOfType(JavaType javaType) {
        if (javaType instanceof JavaClass) {
            return Stream.of((JavaClass) javaType);
        } else if (javaType instanceof JavaParameterizedType) {
            return dependenciesOfParameterizedType((JavaParameterizedType) javaType);
        } else if (javaType instanceof JavaWildcardType) {
            return dependenciesOfWildcardType((JavaWildcardType) javaType);
        }

        return Stream.empty();
    }

    private static Stream<JavaClass> dependenciesOfParameterizedType(JavaParameterizedType parameterizedType) {
        return Stream.concat(
                Stream.of(parameterizedType.toErasure()),
                parameterizedType.getActualTypeArguments().stream().flatMap(JavaClassDependencies::dependenciesOfType)
        );
    }

    private static Stream<JavaClass> dependenciesOfWildcardType(JavaWildcardType javaType) {
        return Stream.concat(javaType.getUpperBounds().stream(), javaType.getLowerBounds().stream())
                .flatMap(JavaClassDependencies::dependenciesOfType);
    }

    private Stream<Dependency> parameterAnnotationDependencies(Set<? extends JavaCodeUnit> codeUnits) {
        return codeUnits.stream().flatMap(codeUnit -> annotationDependencies(codeUnit.getParameters()));
    }

    private <T extends HasDescription & HasAnnotations<?>> Stream<Dependency> annotationDependencies(Collection<T> annotatedObjects) {
        return annotatedObjects.stream().flatMap(this::annotationDependencies);
    }

    private <T extends HasDescription & HasAnnotations<?>> Stream<Dependency> annotationDependencies(T annotated) {
        Stream.Builder<Dependency> addToStream = Stream.builder();
        for (JavaAnnotation<?> annotation : annotated.getAnnotations()) {
            Dependency.tryCreateFromAnnotation(annotation).forEach(addToStream);
            annotation.accept(new DefaultParameterVisitor() {
                @Override
                public void visitClass(String propertyName, JavaClass javaClass) {
                    Dependency.tryCreateFromAnnotationMember(annotation, javaClass).forEach(addToStream);
                }

                @Override
                public void visitEnumConstant(String propertyName, JavaEnumConstant enumConstant) {
                    Dependency.tryCreateFromAnnotationMember(annotation, enumConstant.getDeclaringClass()).forEach(addToStream);
                }

                @Override
                public void visitAnnotation(String propertyName, JavaAnnotation<?> memberAnnotation) {
                    Dependency.tryCreateFromAnnotationMember(annotation, memberAnnotation.getRawType()).forEach(addToStream);
                    memberAnnotation.accept(this);
                }
            });
        }
        return addToStream.build();
    }
}
