/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeParameterBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.TypeParametersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

class ClassFileImportRecord {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFileImportRecord.class);

    private static final TypeParametersBuilder NO_TYPE_PARAMETERS =
            new TypeParametersBuilder(Collections.<JavaTypeParameterBuilder<JavaClass>>emptyList());

    private final Map<String, JavaClass> classes = new HashMap<>();

    private final Map<String, String> superclassNamesByOwner = new HashMap<>();
    private final SetMultimap<String, String> interfaceNamesByOwner = HashMultimap.create();
    private final Map<String, TypeParametersBuilder> typeParametersBuilderByOwner = new HashMap<>();
    private final Map<String, JavaParameterizedTypeBuilder<JavaClass>> genericSuperclassBuilderByOwner = new HashMap<>();
    private final Map<String, Set<JavaParameterizedTypeBuilder<JavaClass>>> genericInterfaceBuildersByOwner = new HashMap<>();
    private final SetMultimap<String, DomainBuilders.JavaFieldBuilder> fieldBuildersByOwner = HashMultimap.create();
    private final SetMultimap<String, DomainBuilders.JavaMethodBuilder> methodBuildersByOwner = HashMultimap.create();
    private final SetMultimap<String, DomainBuilders.JavaConstructorBuilder> constructorBuildersByOwner = HashMultimap.create();
    private final Map<String, DomainBuilders.JavaStaticInitializerBuilder> staticInitializerBuildersByOwner = new HashMap<>();
    private final SetMultimap<String, DomainBuilders.JavaAnnotationBuilder> annotationsByOwner = HashMultimap.create();
    private final Map<String, DomainBuilders.JavaAnnotationBuilder.ValueBuilder> annotationDefaultValuesByOwner = new HashMap<>();
    private final EnclosingClassesByInnerClasses enclosingClassNamesByOwner = new EnclosingClassesByInnerClasses();

    private final Set<RawAccessRecord.ForField> rawFieldAccessRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawMethodCallRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawConstructorCallRecords = new HashSet<>();

    void setSuperclass(String ownerName, String superclassName) {
        checkState(!superclassNamesByOwner.containsKey(ownerName),
                "Attempted to add %s as a second superclass to %s, this is most likely a bug",
                superclassName, ownerName);
        superclassNamesByOwner.put(ownerName, superclassName);
    }

    void addInterfaces(String ownerName, Set<String> interfaceNames) {
        interfaceNamesByOwner.putAll(ownerName, interfaceNames);
    }

    void addTypeParameters(String ownerName, TypeParametersBuilder builder) {
        typeParametersBuilderByOwner.put(ownerName, builder);
    }

    void addGenericSuperclass(String ownerName, JavaParameterizedTypeBuilder<JavaClass> genericSuperclassBuilder) {
        genericSuperclassBuilderByOwner.put(ownerName, genericSuperclassBuilder);
    }

    public void addGenericInterfaces(String ownerName, Set<JavaParameterizedTypeBuilder<JavaClass>> genericInterfaceBuilders) {
        genericInterfaceBuildersByOwner.put(ownerName, genericInterfaceBuilders);
    }

    void addField(String ownerName, DomainBuilders.JavaFieldBuilder fieldBuilder) {
        fieldBuildersByOwner.put(ownerName, fieldBuilder);
    }

    void addMethod(String ownerName, DomainBuilders.JavaMethodBuilder methodBuilder) {
        methodBuildersByOwner.put(ownerName, methodBuilder);
    }

    void addConstructor(String ownerName, DomainBuilders.JavaConstructorBuilder constructorBuilder) {
        constructorBuildersByOwner.put(ownerName, constructorBuilder);
    }

    void setStaticInitializer(String ownerName, DomainBuilders.JavaStaticInitializerBuilder builder) {
        checkState(!staticInitializerBuildersByOwner.containsKey(ownerName),
                "Tried to add a second static initializer to %s, this is most likely a bug",
                ownerName);
        staticInitializerBuildersByOwner.put(ownerName, builder);
    }

    void addClassAnnotations(String ownerName, Set<DomainBuilders.JavaAnnotationBuilder> annotations) {
        this.annotationsByOwner.putAll(ownerName, annotations);
    }

    void addMemberAnnotations(String declaringClassName, String memberName, String descriptor, Set<DomainBuilders.JavaAnnotationBuilder> annotations) {
        this.annotationsByOwner.putAll(getMemberKey(declaringClassName, memberName, descriptor), annotations);
    }

    void addAnnotationDefaultValue(String declaringClassName, String methodName, String descriptor, DomainBuilders.JavaAnnotationBuilder.ValueBuilder valueBuilder) {
        annotationDefaultValuesByOwner.put(getMemberKey(declaringClassName, methodName, descriptor), valueBuilder);
    }

    void setEnclosingClass(String ownerName, String enclosingClassName) {
        enclosingClassNamesByOwner.register(ownerName, enclosingClassName);
    }

    Optional<String> getSuperclassFor(String name) {
        return Optional.fromNullable(superclassNamesByOwner.get(name));
    }

    Set<String> getInterfaceNamesFor(String ownerName) {
        return interfaceNamesByOwner.get(ownerName);
    }

    TypeParametersBuilder getTypeParameterBuildersFor(String ownerName) {
        if (!typeParametersBuilderByOwner.containsKey(ownerName)) {
            return NO_TYPE_PARAMETERS;
        }
        return typeParametersBuilderByOwner.get(ownerName);
    }

    Optional<JavaParameterizedTypeBuilder<JavaClass>> getGenericSuperclassFor(JavaClass owner) {
        return Optional.fromNullable(genericSuperclassBuilderByOwner.get(owner.getName()));
    }

    Optional<Set<JavaParameterizedTypeBuilder<JavaClass>>> getGenericInterfacesFor(JavaClass owner) {
        return Optional.fromNullable(genericInterfaceBuildersByOwner.get(owner.getName()));
    }

    Set<String> getMemberSignatureTypeNames() {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (DomainBuilders.JavaFieldBuilder fieldBuilder : fieldBuildersByOwner.values()) {
            result.add(fieldBuilder.getTypeName());
        }
        for (DomainBuilders.JavaConstructorBuilder constructorBuilder : constructorBuildersByOwner.values()) {
            for (String parameterTypeName : constructorBuilder.getParameterTypeNames()) {
                result.add(parameterTypeName);
            }
        }
        for (DomainBuilders.JavaMethodBuilder methodBuilder : methodBuildersByOwner.values()) {
            result.add(methodBuilder.getReturnTypeName());
            for (String parameterTypeName : methodBuilder.getParameterTypeNames()) {
                result.add(parameterTypeName);
            }
        }
        return result.build();
    }

    Set<DomainBuilders.JavaFieldBuilder> getFieldBuildersFor(String ownerName) {
        return fieldBuildersByOwner.get(ownerName);
    }

    Set<DomainBuilders.JavaMethodBuilder> getMethodBuildersFor(String ownerName) {
        return methodBuildersByOwner.get(ownerName);
    }

    Set<DomainBuilders.JavaConstructorBuilder> getConstructorBuildersFor(String ownerName) {
        return constructorBuildersByOwner.get(ownerName);
    }

    Optional<DomainBuilders.JavaStaticInitializerBuilder> getStaticInitializerBuilderFor(String ownerName) {
        return Optional.fromNullable(staticInitializerBuildersByOwner.get(ownerName));
    }

    Set<String> getAnnotationTypeNamesFor(JavaClass owner) {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (DomainBuilders.JavaAnnotationBuilder annotationBuilder : annotationsByOwner.get(owner.getName())) {
            result.add(annotationBuilder.getFullyQualifiedClassName());
        }
        return result.build();
    }

    Set<DomainBuilders.JavaAnnotationBuilder> getAnnotationsFor(JavaClass owner) {
        return annotationsByOwner.get(owner.getName());
    }

    Set<String> getMemberAnnotationTypeNamesFor(JavaClass owner) {
        Iterable<DomainBuilders.JavaMemberBuilder<?, ?>> memberBuilders = Iterables.concat(
                fieldBuildersByOwner.get(owner.getName()),
                methodBuildersByOwner.get(owner.getName()),
                constructorBuildersByOwner.get(owner.getName()),
                nullToEmpty(staticInitializerBuildersByOwner.get(owner.getName())));

        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (DomainBuilders.JavaMemberBuilder<?, ?> memberBuilder : memberBuilders) {
            for (DomainBuilders.JavaAnnotationBuilder annotationBuilder : annotationsByOwner.get(getMemberKey(owner.getName(), memberBuilder.getName(), memberBuilder.getDescriptor()))) {
                result.add(annotationBuilder.getFullyQualifiedClassName());
            }
        }
        return result.build();
    }

    private Iterable<DomainBuilders.JavaMemberBuilder<?, ?>> nullToEmpty(DomainBuilders.JavaStaticInitializerBuilder staticInitializerBuilder) {
        return staticInitializerBuilder != null
                ? Collections.<DomainBuilders.JavaMemberBuilder<?, ?>>singleton(staticInitializerBuilder)
                : Collections.<DomainBuilders.JavaMemberBuilder<?, ?>>emptySet();
    }

    Set<DomainBuilders.JavaAnnotationBuilder> getAnnotationsFor(JavaMember owner) {
        return annotationsByOwner.get(getMemberKey(owner));
    }

    Optional<DomainBuilders.JavaAnnotationBuilder.ValueBuilder> getAnnotationDefaultValueBuilderFor(JavaMethod method) {
        return Optional.fromNullable(annotationDefaultValuesByOwner.get(getMemberKey(method)));
    }

    Optional<String> getEnclosingClassFor(String ownerName) {
        return enclosingClassNamesByOwner.get(ownerName);
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

    Set<RawAccessRecord.ForField> getRawFieldAccessRecords() {
        return ImmutableSet.copyOf(rawFieldAccessRecords);
    }

    Set<RawAccessRecord> getRawMethodCallRecords() {
        return ImmutableSet.copyOf(rawMethodCallRecords);
    }

    Set<RawAccessRecord> getRawConstructorCallRecords() {
        return ImmutableSet.copyOf(rawConstructorCallRecords);
    }

    void addAll(Collection<JavaClass> javaClasses) {
        for (JavaClass javaClass : javaClasses) {
            classes.put(javaClass.getName(), javaClass);
        }
    }

    Map<String, JavaClass> getClasses() {
        return classes;
    }

    Set<RawAccessRecord> getAccessRecords() {
        return ImmutableSet.<RawAccessRecord>builder()
                .addAll(rawFieldAccessRecords)
                .addAll(rawMethodCallRecords)
                .addAll(rawConstructorCallRecords)
                .build();
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

    // NOTE: ASM calls visitInnerClass and visitOuterClass several times, sometimes when the outer class is imported
    //       and sometimes again when the inner class is imported. To make it easier, we'll just deal with duplicate
    //       registrations, as there is no harm, as long as no conflicting information is recorded.
    private static class EnclosingClassesByInnerClasses {
        private final Map<String, String> innerToOuter = new HashMap<>();

        void register(String innerName, String outerName) {
            if (registeringAllowed(innerName, outerName)) {
                innerToOuter.put(innerName, outerName);
            }
        }

        private boolean registeringAllowed(String innerName, String outerName) {
            boolean registeringAllowed = !innerToOuter.containsKey(innerName) ||
                    innerToOuter.get(innerName).equals(outerName);

            if (!registeringAllowed) {
                LOG.warn("Skipping registering outer class {} for inner class {}, since already outer class {} was registered",
                        outerName, innerName, innerToOuter.get(innerName));
            }

            return registeringAllowed;
        }

        public Optional<String> get(String ownerName) {
            return Optional.fromNullable(innerToOuter.get(ownerName));
        }
    }
}
