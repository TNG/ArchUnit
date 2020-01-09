/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.Optional;

@Internal
public interface ImportContext {
    Optional<JavaClass> createSuperClass(JavaClass owner);

    Set<JavaClass> createInterfaces(JavaClass owner);

    Set<JavaField> createFields(JavaClass owner);

    Set<JavaMethod> createMethods(JavaClass owner);

    Set<JavaConstructor> createConstructors(JavaClass owner);

    Optional<JavaStaticInitializer> createStaticInitializer(JavaClass owner);

    Map<String, JavaAnnotation<JavaClass>> createAnnotations(JavaClass owner);

    Optional<JavaClass> createEnclosingClass(JavaClass owner);

    Set<JavaFieldAccess> getFieldAccessesFor(JavaCodeUnit codeUnit);

    Set<JavaMethodCall> getMethodCallsFor(JavaCodeUnit codeUnit);

    Set<JavaConstructorCall> getConstructorCallsFor(JavaCodeUnit codeUnit);

    Set<JavaField> getFieldsOfType(JavaClass javaClass);

    Set<JavaMethod> getMethodsWithParameterOfType(JavaClass javaClass);

    Set<JavaMethod> getMethodsWithReturnType(JavaClass javaClass);

    Set<ThrowsDeclaration<JavaMethod>> getMethodThrowsDeclarationsOfType(JavaClass javaClass);

    Set<JavaConstructor> getConstructorsWithParameterOfType(JavaClass javaClass);

    Set<ThrowsDeclaration<JavaConstructor>> getConstructorThrowsDeclarationsOfType(JavaClass javaClass);

    Set<JavaAnnotation<?>> getAnnotationsOfType(JavaClass javaClass);

    Set<JavaAnnotation<?>> getAnnotationsWithParameterOfType(JavaClass javaClass);

    JavaClass resolveClass(String fullyQualifiedClassName);
}
