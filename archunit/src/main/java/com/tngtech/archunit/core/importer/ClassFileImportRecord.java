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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassTypeParametersBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaStaticInitializerBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.TryCatchBlockBuilder;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.importer.RawAccessRecord.MemberSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.base.Optionals.stream;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporter.isLambdaMethodName;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporter.isSyntheticAccessMethodName;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporter.isSyntheticEnumSwitchMapFieldName;
import static java.util.Collections.emptyList;

class ClassFileImportRecord {
    private static final Logger log = LoggerFactory.getLogger(ClassFileImportRecord.class);

    private static final JavaClassTypeParametersBuilder NO_TYPE_PARAMETERS =
            new JavaClassTypeParametersBuilder(emptyList());

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
    private final SetMultimap<String, TryCatchBlockBuilder> tryCatchBlocksByOwner = HashMultimap.create();

    private final Set<RawAccessRecord.ForField> rawFieldAccessRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawMethodCallRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawConstructorCallRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawMethodReferenceRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawConstructorReferenceRecords = new HashSet<>();
    private final SyntheticAccessRecorder syntheticLambdaAccessRecorder = createSyntheticLambdaAccessRecorder();
    private final SyntheticAccessRecorder syntheticPrivateAccessRecorder = createSyntheticPrivateAccessRecorder();

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

    void addTryCatchBlocks(String declaringClassName, String methodName, String descriptor, Set<TryCatchBlockBuilder> tryCatchBlocks) {
        tryCatchBlocksByOwner.putAll(getMemberKey(declaringClassName, methodName, descriptor), tryCatchBlocks);
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

    Set<JavaAnnotationBuilder> getAnnotationsFor(JavaClass owner) {
        return annotationsByOwner.get(owner.getName());
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

    Set<TryCatchBlockBuilder> getTryCatchBlockBuildersFor(JavaCodeUnit codeUnit) {
        return tryCatchBlocksByOwner.get(getMemberKey(codeUnit));
    }

    void registerFieldAccess(RawAccessRecord.ForField record) {
        if (!isSyntheticEnumSwitchMapFieldName(record.target.name)) {
            rawFieldAccessRecords.add(record);
        }
    }

    void registerMethodCall(RawAccessRecord record) {
        if (isSyntheticAccessMethodName(record.target.name)) {
            syntheticPrivateAccessRecorder.registerSyntheticMethodInvocation(record);
        } else {
            rawMethodCallRecords.add(record);
        }
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

    void registerLambdaInvocation(RawAccessRecord record) {
        syntheticLambdaAccessRecorder.registerSyntheticMethodInvocation(record);
    }

    void forEachRawFieldAccessRecord(Consumer<RawAccessRecord.ForField> doWithRecord) {
        fixSyntheticOrigins(
                rawFieldAccessRecords, COPY_RAW_FIELD_ACCESS_RECORD,
                syntheticPrivateAccessRecorder, syntheticLambdaAccessRecorder
        ).forEach(doWithRecord);
    }

    void forEachRawMethodCallRecord(Consumer<RawAccessRecord> doWithRecord) {
        fixSyntheticOrigins(
                rawMethodCallRecords, COPY_RAW_ACCESS_RECORD,
                syntheticPrivateAccessRecorder, syntheticLambdaAccessRecorder
        ).forEach(doWithRecord);
    }

    void forEachRawConstructorCallRecord(Consumer<RawAccessRecord> doWithRecord) {
        fixSyntheticOrigins(
                rawConstructorCallRecords, COPY_RAW_ACCESS_RECORD,
                syntheticLambdaAccessRecorder
        ).forEach(doWithRecord);
    }

    void forEachRawMethodReferenceRecord(Consumer<RawAccessRecord> doWithRecord) {
        fixSyntheticOrigins(
                rawMethodReferenceRecords, COPY_RAW_ACCESS_RECORD,
                syntheticPrivateAccessRecorder, syntheticLambdaAccessRecorder
        ).forEach(doWithRecord);
    }

    void forEachRawConstructorReferenceRecord(Consumer<RawAccessRecord> doWithRecord) {
        fixSyntheticOrigins(
                rawConstructorReferenceRecords, COPY_RAW_ACCESS_RECORD,
                syntheticLambdaAccessRecorder
        ).forEach(doWithRecord);
    }

    private <ACCESS extends RawAccessRecord> Stream<ACCESS> fixSyntheticOrigins(
            Set<ACCESS> rawAccessRecordsIncludingSyntheticAccesses,
            Function<ACCESS, ? extends RawAccessRecord.BaseBuilder<ACCESS, ?>> createAccessWithNewOrigin,
            SyntheticAccessRecorder... syntheticAccessRecorders
    ) {

        Stream<ACCESS> result = rawAccessRecordsIncludingSyntheticAccesses.stream();
        for (SyntheticAccessRecorder syntheticAccessRecorder : syntheticAccessRecorders) {
            result = result.flatMap(access -> stream(syntheticAccessRecorder.fixSyntheticAccess(access, createAccessWithNewOrigin)));
        }
        return result;
    }

    void add(JavaClass javaClass) {
        classes.put(javaClass.getName(), javaClass);
    }

    Map<String, JavaClass> getClasses() {
        return classes;
    }

    private static final Function<RawAccessRecord, RawAccessRecord.Builder> COPY_RAW_ACCESS_RECORD =
            access -> new RawAccessRecord.Builder()
                    .withCaller(access.caller)
                    .withTarget(access.target)
                    .withLineNumber(access.lineNumber)
                    .withDeclaredInLambda(access.declaredInLambda);

    private static final Function<RawAccessRecord.ForField, RawAccessRecord.ForField.Builder> COPY_RAW_FIELD_ACCESS_RECORD =
            access -> new RawAccessRecord.ForField.Builder()
                    .withCaller(access.caller)
                    .withAccessType(access.accessType)
                    .withTarget(access.target)
                    .withLineNumber(access.lineNumber)
                    .withDeclaredInLambda(access.declaredInLambda);

    private static SyntheticAccessRecorder createSyntheticLambdaAccessRecorder() {
        return new SyntheticAccessRecorder(
                codeUnit -> isLambdaMethodName(codeUnit.getName()),
                (accessBuilder, newOrigin) -> accessBuilder
                        .withCaller(newOrigin)
                        .withDeclaredInLambda(true)
        );
    }

    private static SyntheticAccessRecorder createSyntheticPrivateAccessRecorder() {
        return new SyntheticAccessRecorder(
                codeUnit -> isSyntheticAccessMethodName(codeUnit.getName()),
                RawAccessRecord.BaseBuilder::withCaller
        );
    }

    private static String getMemberKey(MemberSignature member) {
        return getMemberKey(member.getDeclaringClassName(), member.getName(), member.getDescriptor());
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

    private static class SyntheticAccessRecorder {
        private final Map<String, RawAccessRecord> rawSyntheticMethodInvocationRecordsByTarget = new HashMap<>();
        private final Predicate<CodeUnit> isSyntheticOrigin;
        private final BiConsumer<RawAccessRecord.BaseBuilder<?, ?>, CodeUnit> fixOrigin;

        SyntheticAccessRecorder(
                Predicate<CodeUnit> isSyntheticOrigin,
                BiConsumer<RawAccessRecord.BaseBuilder<?, ?>, CodeUnit> fixOrigin
        ) {
            this.isSyntheticOrigin = isSyntheticOrigin;
            this.fixOrigin = fixOrigin;
        }

        void registerSyntheticMethodInvocation(RawAccessRecord record) {
            rawSyntheticMethodInvocationRecordsByTarget.put(getMemberKey(record.target), record);
        }

        <ACCESS extends RawAccessRecord> Optional<ACCESS> fixSyntheticAccess(
                ACCESS access,
                Function<ACCESS, ? extends RawAccessRecord.BaseBuilder<ACCESS, ?>> copyAccess
        ) {
            return isSyntheticOrigin.test(access.caller)
                    ? replaceOriginByFixedOrigin(access, copyAccess)
                    : Optional.of(access);
        }

        private <ACCESS extends RawAccessRecord> Optional<ACCESS> replaceOriginByFixedOrigin(
                ACCESS accessFromSyntheticMethod,
                Function<ACCESS, ? extends RawAccessRecord.BaseBuilder<ACCESS, ?>> copyAccess
        ) {
            RawAccessRecord accessWithCorrectOrigin = findNonSyntheticOriginOf(accessFromSyntheticMethod);

            if (accessWithCorrectOrigin != null) {
                RawAccessRecord.BaseBuilder<ACCESS, ?> copiedBuilder = copyAccess.apply(accessFromSyntheticMethod);
                fixOrigin.accept(copiedBuilder, accessWithCorrectOrigin.caller);
                return Optional.of(copiedBuilder.build());
            } else {
                log.warn("Could not find matching origin for synthetic method {}.{}|{}",
                        accessFromSyntheticMethod.target.getDeclaringClassName(),
                        accessFromSyntheticMethod.target.name,
                        accessFromSyntheticMethod.target.getDescriptor());
                return Optional.empty();
            }
        }

        private <ACCESS extends RawAccessRecord> RawAccessRecord findNonSyntheticOriginOf(ACCESS accessFromSyntheticMethod) {
            RawAccessRecord result = accessFromSyntheticMethod;
            do {
                result = rawSyntheticMethodInvocationRecordsByTarget.get(getMemberKey(result.caller));
            } while (result != null && isSyntheticOrigin.test(result.caller));

            return result;
        }
    }
}
