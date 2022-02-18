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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassTypeParametersBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaCodeUnitBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMemberBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaStaticInitializerBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeParameterBuilder;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

class ClassFileImportRecord {
    private static final JavaClassTypeParametersBuilder NO_TYPE_PARAMETERS =
            new JavaClassTypeParametersBuilder(Collections.<JavaTypeParameterBuilder<JavaClass>>emptyList());

    private final Map<String, JavaClass> classes = new HashMap<>();

    private final Map<String, String> superclassNamesByOwner = new HashMap<>();
    private final ListMultimap<String, String> interfaceNamesByOwner = ArrayListMultimap.create();
    private final Map<String, JavaClassTypeParametersBuilder> typeParametersBuilderByOwner = new HashMap<>();
    private final Map<String, JavaParameterizedTypeBuilder<JavaClass>> genericSuperclassBuilderByOwner = new HashMap<>();
    private final Map<String, List<JavaParameterizedTypeBuilder<JavaClass>>> genericInterfaceBuildersByOwner = new HashMap<>();
    private final SetMultimap<String, JavaFieldBuilder> fieldBuildersByOwner = HashMultimap.create();
    private final SetMultimap<String, JavaMethodBuilder> methodBuildersByOwner = HashMultimap.create();
    private final SetMultimap<String, JavaConstructorBuilder> constructorBuildersByOwner = HashMultimap.create();
    private final Map<String, JavaStaticInitializerBuilder> staticInitializerBuildersByOwner = new HashMap<>();
    private final SetMultimap<String, JavaAnnotationBuilder> annotationsByOwner = HashMultimap.create();
    private final Map<String, JavaAnnotationBuilder.ValueBuilder> annotationDefaultValuesByOwner = new HashMap<>();
    private final EnclosingDeclarationsByInnerClasses enclosingDeclarationsByOwner = new EnclosingDeclarationsByInnerClasses();

    private final Set<RawAccessRecord.ForField> rawFieldAccessRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawMethodCallRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawConstructorCallRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawMethodReferenceRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawConstructorReferenceRecords = new HashSet<>();

    void setSuperclass(String ownerName, String superclassName) {
        checkState(!superclassNamesByOwner.containsKey(ownerName),
                "Attempted to add %s as a second superclass to %s, this is most likely a bug",
                superclassName, ownerName);
        superclassNamesByOwner.put(ownerName, superclassName);
    }

    void addInterfaces(String ownerName, List<String> interfaceNames) {
        interfaceNamesByOwner.putAll(ownerName, interfaceNames);
    }

    void addTypeParameters(String ownerName, JavaClassTypeParametersBuilder builder) {
        typeParametersBuilderByOwner.put(ownerName, builder);
    }

    void addGenericSuperclass(String ownerName, JavaParameterizedTypeBuilder<JavaClass> genericSuperclassBuilder) {
        genericSuperclassBuilderByOwner.put(ownerName, genericSuperclassBuilder);
    }

    public void addGenericInterfaces(String ownerName, List<JavaParameterizedTypeBuilder<JavaClass>> genericInterfaceBuilders) {
        genericInterfaceBuildersByOwner.put(ownerName, genericInterfaceBuilders);
    }

    void addField(String ownerName, JavaFieldBuilder fieldBuilder) {
        fieldBuildersByOwner.put(ownerName, fieldBuilder);
    }

    void addMethod(String ownerName, JavaMethodBuilder methodBuilder) {
        methodBuildersByOwner.put(ownerName, methodBuilder);
    }

    void addConstructor(String ownerName, JavaConstructorBuilder constructorBuilder) {
        constructorBuildersByOwner.put(ownerName, constructorBuilder);
    }

    void setStaticInitializer(String ownerName, JavaStaticInitializerBuilder builder) {
        checkState(!staticInitializerBuildersByOwner.containsKey(ownerName),
                "Tried to add a second static initializer to %s, this is most likely a bug",
                ownerName);
        staticInitializerBuildersByOwner.put(ownerName, builder);
    }

    void addClassAnnotations(String ownerName, Set<JavaAnnotationBuilder> annotations) {
        this.annotationsByOwner.putAll(ownerName, annotations);
    }

    void addMemberAnnotations(String declaringClassName, String memberName, String descriptor, Set<JavaAnnotationBuilder> annotations) {
        this.annotationsByOwner.putAll(getMemberKey(declaringClassName, memberName, descriptor), annotations);
    }

    void addAnnotationDefaultValue(String declaringClassName, String methodName, String descriptor, JavaAnnotationBuilder.ValueBuilder valueBuilder) {
        annotationDefaultValuesByOwner.put(getMemberKey(declaringClassName, methodName, descriptor), valueBuilder);
    }

    void setEnclosingClass(String ownerName, String enclosingClassName) {
        enclosingDeclarationsByOwner.registerEnclosingClass(ownerName, enclosingClassName);
    }

    void setEnclosingCodeUnit(String ownerName, CodeUnit enclosingCodeUnit) {
        enclosingDeclarationsByOwner.registerEnclosingCodeUnit(ownerName, enclosingCodeUnit);
    }

    Optional<String> getSuperclassFor(String name) {
        return Optional.ofNullable(superclassNamesByOwner.get(name));
    }

    List<String> getInterfaceNamesFor(String ownerName) {
        return interfaceNamesByOwner.get(ownerName);
    }

    JavaClassTypeParametersBuilder getTypeParameterBuildersFor(String ownerName) {
        if (!typeParametersBuilderByOwner.containsKey(ownerName)) {
            return NO_TYPE_PARAMETERS;
        }
        return typeParametersBuilderByOwner.get(ownerName);
    }

    Optional<JavaParameterizedTypeBuilder<JavaClass>> getGenericSuperclassFor(JavaClass owner) {
        return Optional.ofNullable(genericSuperclassBuilderByOwner.get(owner.getName()));
    }

    Optional<List<JavaParameterizedTypeBuilder<JavaClass>>> getGenericInterfacesFor(JavaClass owner) {
        return Optional.ofNullable(genericInterfaceBuildersByOwner.get(owner.getName()));
    }

    Set<JavaFieldBuilder> getFieldBuildersFor(String ownerName) {
        return fieldBuildersByOwner.get(ownerName);
    }

    Set<JavaMethodBuilder> getMethodBuildersFor(String ownerName) {
        return methodBuildersByOwner.get(ownerName);
    }

    Set<JavaConstructorBuilder> getConstructorBuildersFor(String ownerName) {
        return constructorBuildersByOwner.get(ownerName);
    }

    Optional<JavaStaticInitializerBuilder> getStaticInitializerBuilderFor(String ownerName) {
        return Optional.ofNullable(staticInitializerBuildersByOwner.get(ownerName));
    }

    Set<String> getAnnotationTypeNamesFor(JavaClass owner) {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (JavaAnnotationBuilder annotationBuilder : annotationsByOwner.get(owner.getName())) {
            result.add(annotationBuilder.getFullyQualifiedClassName());
        }
        return result.build();
    }

    Set<JavaAnnotationBuilder> getAnnotationsFor(JavaClass owner) {
        return annotationsByOwner.get(owner.getName());
    }

    Set<String> getMemberAnnotationTypeNamesFor(JavaClass owner) {
        Iterable<JavaMemberBuilder<?, ?>> memberBuilders = Iterables.concat(
                fieldBuildersByOwner.get(owner.getName()),
                methodBuildersByOwner.get(owner.getName()),
                constructorBuildersByOwner.get(owner.getName()),
                nullToEmpty(staticInitializerBuildersByOwner.get(owner.getName())));

        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (JavaMemberBuilder<?, ?> memberBuilder : memberBuilders) {
            for (JavaAnnotationBuilder annotationBuilder : annotationsByOwner.get(getMemberKey(owner.getName(), memberBuilder.getName(), memberBuilder.getDescriptor()))) {
                result.add(annotationBuilder.getFullyQualifiedClassName());
            }
        }
        return result.build();
    }

    Set<String> getParameterAnnotationTypeNamesFor(JavaClass owner) {
        Iterable<JavaCodeUnitBuilder<?, ?>> codeUnitBuilders = Iterables.<JavaCodeUnitBuilder<?, ?>>concat(
                methodBuildersByOwner.get(owner.getName()),
                constructorBuildersByOwner.get(owner.getName()));

        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (JavaCodeUnitBuilder<?, ?> codeUnitBuilder : codeUnitBuilders) {
            for (JavaAnnotationBuilder annotationBuilder : codeUnitBuilder.getParameterAnnotationBuilders()) {
                result.add(annotationBuilder.getFullyQualifiedClassName());
            }
        }
        return result.build();
    }

    private Iterable<JavaMemberBuilder<?, ?>> nullToEmpty(JavaStaticInitializerBuilder staticInitializerBuilder) {
        return staticInitializerBuilder != null
                ? Collections.<JavaMemberBuilder<?, ?>>singleton(staticInitializerBuilder)
                : Collections.<JavaMemberBuilder<?, ?>>emptySet();
    }

    Set<JavaAnnotationBuilder> getAnnotationsFor(JavaMember owner) {
        return annotationsByOwner.get(getMemberKey(owner));
    }

    Optional<JavaAnnotationBuilder.ValueBuilder> getAnnotationDefaultValueBuilderFor(JavaMethod method) {
        return Optional.ofNullable(annotationDefaultValuesByOwner.get(getMemberKey(method)));
    }

    Optional<String> getEnclosingClassFor(String ownerName) {
        return enclosingDeclarationsByOwner.getEnclosingClassName(ownerName);
    }

    Optional<CodeUnit> getEnclosingCodeUnitFor(String ownerName) {
        return enclosingDeclarationsByOwner.getEnclosingCodeUnit(ownerName);
    }

    void registerFieldAccess(RawAccessRecord.ForField record) {
        rawFieldAccessRecords.add(record);
    }

    void registerMethodCall(RawAccessRecord record) {
        rawMethodCallRecords.add(record);
    }

    void registerConstructorCall(RawAccessRecord record) {
        rawConstructorCallRecords.add(record);
    }

    void registerMethodReference(RawAccessRecord record) {
        rawMethodReferenceRecords.add(record);
    }

    void registerConstructorReference(RawAccessRecord record) {
        rawConstructorReferenceRecords.add(record);
    }

    Set<RawAccessRecord.ForField> getRawFieldAccessRecords() {
        return ImmutableSet.copyOf(rawFieldAccessRecords);
    }

    Set<RawAccessRecord> getRawMethodCallRecords() {
        return ImmutableSet.copyOf(rawMethodCallRecords);
    }

    Set<RawAccessRecord> getRawConstructorCallRecords() {
        return ImmutableSet.copyOf(rawConstructorCallRecords);
    }

    Set<RawAccessRecord> getRawMethodReferenceRecords() {
        return ImmutableSet.copyOf(rawMethodReferenceRecords);
    }

    Set<RawAccessRecord> getRawConstructorReferenceRecords() {
        return ImmutableSet.copyOf(rawConstructorReferenceRecords);
    }

    void addAll(Collection<JavaClass> javaClasses) {
        for (JavaClass javaClass : javaClasses) {
            classes.put(javaClass.getName(), javaClass);
        }
    }

    Map<String, JavaClass> getClasses() {
        return classes;
    }

    Set<String> getAllSuperclassNames() {
        return ImmutableSet.copyOf(superclassNamesByOwner.values());
    }

    Set<String> getAllSuperinterfaceNames() {
        return ImmutableSet.copyOf(interfaceNamesByOwner.values());
    }

    private static String getMemberKey(JavaMember member) {
        return getMemberKey(member.getOwner().getName(), member.getName(), member.getDescriptor());
    }

    private static String getMemberKey(String declaringClassName, String methodName, String descriptor) {
        return declaringClassName + "|" + methodName + "|" + descriptor;
    }

    private static class EnclosingDeclarationsByInnerClasses {
        private final Map<String, String> innerClassNameToEnclosingClassName = new HashMap<>();
        private final Map<String, CodeUnit> innerClassNameToEnclosingCodeUnit = new HashMap<>();

        void registerEnclosingClass(String innerName, String outerName) {
            checkArgument(!innerClassNameToEnclosingClassName.containsKey(innerName)
                            || innerClassNameToEnclosingClassName.get(innerName).equals(outerName),
                    "Can't register multiple enclosing classes, this is likely a bug!");

            innerClassNameToEnclosingClassName.put(innerName, outerName);
        }

        void registerEnclosingCodeUnit(String innerName, CodeUnit codeUnit) {
            checkArgument(!innerClassNameToEnclosingCodeUnit.containsKey(innerName)
                            || innerClassNameToEnclosingCodeUnit.get(innerName).equals(codeUnit),
                    "Can't register multiple enclosing code units, this is likely a bug!");

            innerClassNameToEnclosingCodeUnit.put(innerName, codeUnit);
        }

        Optional<String> getEnclosingClassName(String ownerName) {
            return Optional.ofNullable(innerClassNameToEnclosingClassName.get(ownerName));
        }

        Optional<CodeUnit> getEnclosingCodeUnit(String ownerName) {
            return Optional.ofNullable(innerClassNameToEnclosingCodeUnit.get(ownerName));
        }
    }
}
