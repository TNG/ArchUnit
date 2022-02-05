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
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;

interface DeclarationHandler {
    boolean isNew(String className);

    void onNewClass(String className, Optional<String> superclassName, List<String> interfaceNames);

    void onDeclaredTypeParameters(DomainBuilders.JavaClassTypeParametersBuilder typeParametersBuilder);

    void onGenericSuperclass(DomainBuilders.JavaParameterizedTypeBuilder<JavaClass> genericSuperclassBuilder);

    void onGenericInterfaces(List<DomainBuilders.JavaParameterizedTypeBuilder<JavaClass>> genericInterfaceBuilders);

    void onDeclaredField(DomainBuilders.JavaFieldBuilder fieldBuilder, String fieldTypeName);

    void onDeclaredConstructor(DomainBuilders.JavaConstructorBuilder constructorBuilder, Collection<String> rawParameterTypeNames);

    void onDeclaredMethod(DomainBuilders.JavaMethodBuilder methodBuilder, Collection<String> rawParameterTypeNames, String rawReturnTypeName);

    void onDeclaredStaticInitializer(DomainBuilders.JavaStaticInitializerBuilder staticInitializerBuilder);

    void onDeclaredClassAnnotations(Set<DomainBuilders.JavaAnnotationBuilder> annotationBuilders);

    void onDeclaredMemberAnnotations(String memberName, String descriptor, Set<DomainBuilders.JavaAnnotationBuilder> annotations);

    void onDeclaredAnnotationValueType(String valueTypeName);

    void onDeclaredAnnotationDefaultValue(String methodName, String methodDescriptor, DomainBuilders.JavaAnnotationBuilder.ValueBuilder valueBuilder);

    void registerEnclosingClass(String ownerName, String enclosingClassName);

    void registerEnclosingCodeUnit(String ownerName, RawAccessRecord.CodeUnit enclosingCodeUnit);

    void onDeclaredClassObject(String typeName);

    void onDeclaredInstanceofCheck(String typeName);

    void onDeclaredThrowsClause(Collection<String> exceptionTypeNames);

    void onDeclaredGenericSignatureType(String typeName);
}
