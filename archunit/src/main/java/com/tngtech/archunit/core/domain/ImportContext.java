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
package com.tngtech.archunit.core.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.Optional;

@Internal
public interface ImportContext {
    Optional<JavaClass> createSuperclass(JavaClass owner);

    Optional<JavaType> createGenericSuperclass(JavaClass owner);

    Optional<List<JavaType>> createGenericInterfaces(JavaClass owner);

    List<JavaClass> createInterfaces(JavaClass owner);

    List<JavaTypeVariable<JavaClass>> createTypeParameters(JavaClass owner);

    Set<JavaField> createFields(JavaClass owner);

    Set<JavaMethod> createMethods(JavaClass owner);

    Set<JavaConstructor> createConstructors(JavaClass owner);

    Optional<JavaStaticInitializer> createStaticInitializer(JavaClass owner);

    Map<String, JavaAnnotation<JavaClass>> createAnnotations(JavaClass owner);

    Map<String, JavaAnnotation<JavaMember>> createAnnotations(JavaMember owner);

    Optional<JavaClass> createEnclosingClass(JavaClass owner);

    Optional<JavaCodeUnit> createEnclosingCodeUnit(JavaClass owner);

    Set<JavaFieldAccess> createFieldAccessesFor(JavaCodeUnit codeUnit);

    Set<JavaMethodCall> createMethodCallsFor(JavaCodeUnit codeUnit);

    Set<JavaConstructorCall> createConstructorCallsFor(JavaCodeUnit codeUnit);

    JavaClass resolveClass(String fullyQualifiedClassName);
}
