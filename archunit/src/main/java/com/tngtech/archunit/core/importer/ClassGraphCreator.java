/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorReferenceTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodReferenceTarget;
import com.tngtech.archunit.core.domain.ImportContext;
import com.tngtech.archunit.core.domain.InstanceofCheck;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaConstructorReference;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaMethodReference;
import com.tngtech.archunit.core.domain.JavaStaticInitializer;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.domain.ReferencedClassObject;
import com.tngtech.archunit.core.importer.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassTypeParametersBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorReferenceBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldAccessBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodReferenceBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.TryCatchBlockBuilder;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeAnnotations;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeClassHierarchy;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeEnclosingDeclaration;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeGenericInterfaces;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeGenericSuperclass;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeMembers;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeTypeParameters;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createInstanceofCheck;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createJavaClasses;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createReferencedClassObject;
import static com.tngtech.archunit.core.importer.DomainBuilders.BuilderWithBuildParameter.BuildFinisher.build;
import static com.tngtech.archunit.core.importer.DomainBuilders.buildAnnotations;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporter.isLambdaMethodName;
import static com.tngtech.archunit.core.importer.JavaClassDescriptorImporter.isSyntheticAccessMethodName;

class ClassGraphCreator implements ImportContext {
    private final ImportedClasses classes;

    private final ClassFileImportRecord importRecord;
    private final DependencyResolutionProcess dependencyResolutionProcess;

    private final SetMultimap<JavaCodeUnit, FieldAccessRecord> processedFieldAccessRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<MethodCallTarget>> processedMethodCallRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<ConstructorCallTarget>> processedConstructorCallRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<MethodReferenceTarget>> processedMethodReferenceRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<ConstructorReferenceTarget>> processedConstructorReferenceRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, ReferencedClassObject> processedReferencedClassObjects = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, InstanceofCheck> processedInstanceofChecks = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, TryCatchBlockBuilder> processedTryCatchBlocks = HashMultimap.create();

    ClassGraphCreator(ClassFileImportRecord importRecord, DependencyResolutionProcess dependencyResolutionProcess, ClassResolver classResolver) {
        this.importRecord = importRecord;
        this.dependencyResolutionProcess = dependencyResolutionProcess;
        classes = new ImportedClasses(importRecord.getClasses(), classResolver, this::getMethodReturnType);
    }

    JavaClasses complete() {
        dependencyResolutionProcess.resolve(classes);
        completeClasses();
        completeCodeUnitDependencies();
        return createJavaClasses(classes.getDirectlyImported(), classes.getAllWithOuterClassesSortedBeforeInnerClasses(), this);
    }

    private void completeClasses() {
        for (JavaClass javaClass : classes.getAllWithOuterClassesSortedBeforeInnerClasses()) {
            completeClassHierarchy(javaClass, this);
            completeEnclosingDeclaration(javaClass, this);
            completeTypeParameters(javaClass, this);
            completeGenericSuperclass(javaClass, this);
            completeGenericInterfaces(javaClass, this);
            completeMembers(javaClass, this);
            completeAnnotations(javaClass, this);
        }
    }

    private void completeCodeUnitDependencies() {
        importRecord.forEachRawFieldAccessRecord(record ->
                tryProcess(record, AccessRecord.Factory.forFieldAccessRecord(), processedFieldAccessRecords));
        importRecord.forEachRawMethodCallRecord(record ->
                tryProcess(record, AccessRecord.Factory.forMethodCallRecord(), processedMethodCallRecords));
        importRecord.forEachRawConstructorCallRecord(record ->
                tryProcess(record, AccessRecord.Factory.forConstructorCallRecord(), processedConstructorCallRecords));
        importRecord.forEachRawMethodReferenceRecord(record ->
                tryProcess(record, AccessRecord.Factory.forMethodReferenceRecord(), processedMethodReferenceRecords));
        importRecord.forEachRawConstructorReferenceRecord(record ->
                tryProcess(record, AccessRecord.Factory.forConstructorReferenceRecord(), processedConstructorReferenceRecords));
        importRecord.forEachRawReferencedClassObject(this::processReferencedClassObject);
        importRecord.forEachRawInstanceofCheck(this::processInstanceofCheck);
        importRecord.forEachRawTryCatchBlock(this::processTryCatchBlock);
    }

    private <T extends AccessRecord<?>, B extends RawAccessRecord> void tryProcess(
            B rawRecord,
            AccessRecord.Factory<B, T> factory,
            Multimap<JavaCodeUnit, T> processedAccessRecords) {

        T processed = factory.create(rawRecord, classes);
        processedAccessRecords.put(processed.getOrigin(), processed);
    }

    private void processReferencedClassObject(RawReferencedClassObject rawReferencedClassObject) {
        JavaCodeUnit origin = rawReferencedClassObject.getOrigin().resolveFrom(classes);
        ReferencedClassObject referencedClassObject = createReferencedClassObject(
                origin,
                classes.getOrResolve(rawReferencedClassObject.getClassName()),
                rawReferencedClassObject.getLineNumber(),
                rawReferencedClassObject.isDeclaredInLambda()
        );
        processedReferencedClassObjects.put(origin, referencedClassObject);
    }

    private void processInstanceofCheck(RawInstanceofCheck rawInstanceofCheck) {
        JavaCodeUnit origin = rawInstanceofCheck.getOrigin().resolveFrom(classes);
        InstanceofCheck instanceofCheck = createInstanceofCheck(
                origin,
                classes.getOrResolve(rawInstanceofCheck.getTarget().getFullyQualifiedClassName()),
                rawInstanceofCheck.getLineNumber(),
                rawInstanceofCheck.isDeclaredInLambda()
        );
        processedInstanceofChecks.put(origin, instanceofCheck);
    }

    private void processTryCatchBlock(RawTryCatchBlock rawTryCatchBlock) {
        JavaCodeUnit declaringCodeUnit = rawTryCatchBlock.getDeclaringCodeUnit().resolveFrom(classes);
        TryCatchBlockBuilder tryCatchBlockBuilder = new TryCatchBlockBuilder()
                .withCaughtThrowables(
                        rawTryCatchBlock.getCaughtThrowables().stream()
                                .map(it -> classes.getOrResolve(it.getFullyQualifiedClassName()))
                                .collect(toImmutableSet())
                )
                .withLineNumber(rawTryCatchBlock.getLineNumber())
                .withRawAccessesContainedInTryBlock(rawTryCatchBlock.getAccessesInTryBlock())
                .withDeclaredInLambda(rawTryCatchBlock.isDeclaredInLambda());
        processedTryCatchBlocks.put(declaringCodeUnit, tryCatchBlockBuilder);
    }

    @Override
    public Set<JavaFieldAccess> createFieldAccessesFor(JavaCodeUnit codeUnit, Set<TryCatchBlockBuilder> tryCatchBlockBuilders) {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (FieldAccessRecord record : processedFieldAccessRecords.get(codeUnit)) {
            JavaFieldAccess access = accessBuilderFrom(new JavaFieldAccessBuilder(), record)
                    .withAccessType(record.getAccessType())
                    .build();
            result.add(access);
            handlePossibleTryBlockAccess(tryCatchBlockBuilders, record, access);
        }
        return result.build();
    }

    @Override
    public Set<JavaMethodCall> createMethodCallsFor(JavaCodeUnit codeUnit, Set<TryCatchBlockBuilder> tryCatchBlockBuilders) {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (AccessRecord<MethodCallTarget> record : processedMethodCallRecords.get(codeUnit)) {
            JavaMethodCall call = accessBuilderFrom(new JavaMethodCallBuilder(), record).build();
            result.add(call);
            handlePossibleTryBlockAccess(tryCatchBlockBuilders, record, call);
        }
        return result.build();
    }

    @Override
    public Set<JavaConstructorCall> createConstructorCallsFor(JavaCodeUnit codeUnit, Set<TryCatchBlockBuilder> tryCatchBlockBuilders) {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (AccessRecord<ConstructorCallTarget> record : processedConstructorCallRecords.get(codeUnit)) {
            JavaConstructorCall call = accessBuilderFrom(new JavaConstructorCallBuilder(), record).build();
            result.add(call);
            handlePossibleTryBlockAccess(tryCatchBlockBuilders, record, call);
        }
        return result.build();
    }

    @Override
    public Set<JavaMethodReference> createMethodReferencesFor(JavaCodeUnit codeUnit, Set<TryCatchBlockBuilder> tryCatchBlockBuilders) {
        ImmutableSet.Builder<JavaMethodReference> result = ImmutableSet.builder();
        for (AccessRecord<MethodReferenceTarget> record : processedMethodReferenceRecords.get(codeUnit)) {
            JavaMethodReference methodReference = accessBuilderFrom(new JavaMethodReferenceBuilder(), record).build();
            result.add(methodReference);
            handlePossibleTryBlockAccess(tryCatchBlockBuilders, record, methodReference);
        }
        return result.build();
    }

    @Override
    public Set<JavaConstructorReference> createConstructorReferencesFor(JavaCodeUnit codeUnit, Set<TryCatchBlockBuilder> tryCatchBlockBuilders) {
        ImmutableSet.Builder<JavaConstructorReference> result = ImmutableSet.builder();
        for (AccessRecord<ConstructorReferenceTarget> record : processedConstructorReferenceRecords.get(codeUnit)) {
            JavaConstructorReference constructorReference = accessBuilderFrom(new JavaConstructorReferenceBuilder(), record).build();
            result.add(constructorReference);
            handlePossibleTryBlockAccess(tryCatchBlockBuilders, record, constructorReference);
        }
        return result.build();
    }

    private void handlePossibleTryBlockAccess(Set<TryCatchBlockBuilder> tryCatchBlockBuilders, AccessRecord<?> record, JavaAccess<?> access) {
        tryCatchBlockBuilders.forEach(builder -> builder.addIfContainedInTryBlock(record.getRaw(), access));
    }

    private <T extends AccessTarget, B extends DomainBuilders.JavaAccessBuilder<T, B>>
    B accessBuilderFrom(B builder, AccessRecord<T> record) {
        return builder
                .withOrigin(record.getOrigin())
                .withTarget(record.getTarget())
                .withLineNumber(record.getLineNumber())
                .withDeclaredInLambda(record.isDeclaredInLambda());
    }

    @Override
    public Optional<JavaClass> createSuperclass(JavaClass owner) {
        Optional<String> superclassName = importRecord.getSuperclassFor(owner.getName());
        return superclassName.map(classes::getOrResolve);
    }

    @Override
    public Optional<JavaType> createGenericSuperclass(JavaClass owner) {
        Optional<JavaParameterizedTypeBuilder<JavaClass>> genericSuperclassBuilder = importRecord.getGenericSuperclassFor(owner);
        return genericSuperclassBuilder.map(javaClassJavaParameterizedTypeBuilder ->
                javaClassJavaParameterizedTypeBuilder.build(owner, getTypeParametersInContextOf(owner), classes));
    }

    @Override
    public Optional<List<JavaType>> createGenericInterfaces(JavaClass owner) {
        Optional<List<JavaParameterizedTypeBuilder<JavaClass>>> genericInterfaceBuilders = importRecord.getGenericInterfacesFor(owner);
        if (!genericInterfaceBuilders.isPresent()) {
            return Optional.empty();
        }

        ImmutableList.Builder<JavaType> result = ImmutableList.builder();
        for (JavaParameterizedTypeBuilder<JavaClass> builder : genericInterfaceBuilders.get()) {
            result.add(builder.build(owner, getTypeParametersInContextOf(owner), classes));
        }
        return Optional.of(result.build());
    }

    private static Iterable<JavaTypeVariable<?>> getTypeParametersInContextOf(JavaClass javaClass) {
        Set<JavaTypeVariable<?>> result = Sets.newHashSet(javaClass.getTypeParameters());
        while (javaClass.getEnclosingClass().isPresent()) {
            javaClass = javaClass.getEnclosingClass().get();
            result.addAll(javaClass.getTypeParameters());
        }
        return result;
    }

    @Override
    public List<JavaClass> createInterfaces(JavaClass owner) {
        ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
        for (String interfaceName : importRecord.getInterfaceNamesFor(owner.getName())) {
            result.add(classes.getOrResolve(interfaceName));
        }
        return result.build();
    }

    @Override
    public List<JavaTypeVariable<JavaClass>> createTypeParameters(JavaClass owner) {
        JavaClassTypeParametersBuilder typeParametersBuilder = importRecord.getTypeParameterBuildersFor(owner.getName());
        return typeParametersBuilder.build(owner, classes);
    }

    @Override
    public Set<JavaField> createFields(JavaClass owner) {
        return build(importRecord.getFieldBuildersFor(owner.getName()), owner, classes);
    }

    @Override
    public Set<JavaMethod> createMethods(JavaClass owner) {
        Stream<JavaMethodBuilder> methodBuilders = getNonSyntheticMethodBuildersFor(owner);
        if (owner.isAnnotation()) {
            methodBuilders = methodBuilders.map(methodBuilder -> methodBuilder
                    .withAnnotationDefaultValue(method ->
                            importRecord.getAnnotationDefaultValueBuilderFor(method)
                                    .flatMap(builder -> builder.build(method, classes))
                    ));
        }
        return build(methodBuilders, owner, classes);
    }

    private Stream<JavaMethodBuilder> getNonSyntheticMethodBuildersFor(JavaClass owner) {
        return importRecord.getMethodBuildersFor(owner.getName()).stream()
                .filter(methodBuilder ->
                        !isLambdaMethodName(methodBuilder.getName())
                                && !isSyntheticAccessMethodName(methodBuilder.getName()));
    }

    @Override
    public Set<JavaConstructor> createConstructors(JavaClass owner) {
        return build(importRecord.getConstructorBuildersFor(owner.getName()), owner, classes);
    }

    @Override
    public Optional<JavaStaticInitializer> createStaticInitializer(JavaClass owner) {
        Optional<DomainBuilders.JavaStaticInitializerBuilder> builder = importRecord.getStaticInitializerBuilderFor(owner.getName());
        if (!builder.isPresent()) {
            return Optional.empty();
        }
        JavaStaticInitializer staticInitializer = builder.get().build(owner, classes);
        return Optional.of(staticInitializer);
    }

    @Override
    public Map<String, JavaAnnotation<JavaClass>> createAnnotations(JavaClass owner) {
        return createAnnotations(owner, importRecord.getAnnotationsFor(owner));
    }

    @Override
    public Map<String, JavaAnnotation<JavaMember>> createAnnotations(JavaMember owner) {
        return createAnnotations(owner, importRecord.getAnnotationsFor(owner));
    }

    private <OWNER extends HasDescription> Map<String, JavaAnnotation<OWNER>> createAnnotations(OWNER owner, Set<DomainBuilders.JavaAnnotationBuilder> annotationBuilders) {
        return buildAnnotations(owner, annotationBuilders, classes);
    }

    @Override
    public Optional<JavaClass> createEnclosingClass(JavaClass owner) {
        Optional<String> enclosingClassName = importRecord.getEnclosingClassFor(owner.getName());
        return enclosingClassName.map(classes::getOrResolve);
    }

    @Override
    public Optional<JavaCodeUnit> createEnclosingCodeUnit(JavaClass owner) {
        Optional<CodeUnit> enclosingCodeUnit = importRecord.getEnclosingCodeUnitFor(owner.getName());
        if (!enclosingCodeUnit.isPresent()) {
            return Optional.empty();
        }

        CodeUnit codeUnit = enclosingCodeUnit.get();
        JavaClass enclosingClass = classes.getOrResolve(codeUnit.getDeclaringClassName());
        return enclosingClass.tryGetCodeUnitWithParameterTypeNames(codeUnit.getName(), codeUnit.getRawParameterTypeNames());
    }

    @Override
    public Set<TryCatchBlockBuilder> createTryCatchBlockBuilders(JavaCodeUnit codeUnit) {
        return processedTryCatchBlocks.get(codeUnit);
    }

    @Override
    public Set<ReferencedClassObject> createReferencedClassObjectsFor(JavaCodeUnit codeUnit) {
        return ImmutableSet.copyOf(processedReferencedClassObjects.get(codeUnit));
    }

    @Override
    public Set<InstanceofCheck> createInstanceofChecksFor(JavaCodeUnit codeUnit) {
        return ImmutableSet.copyOf(processedInstanceofChecks.get(codeUnit));
    }

    @Override
    public JavaClass resolveClass(String fullyQualifiedClassName) {
        return classes.getOrResolve(fullyQualifiedClassName);
    }

    private Optional<JavaClass> getMethodReturnType(String declaringClassName, String methodName) {
        for (JavaMethodBuilder methodBuilder : importRecord.getMethodBuildersFor(declaringClassName)) {
            if (methodBuilder.getName().equals(methodName) && methodBuilder.hasNoParameters()) {
                return Optional.of(classes.getOrResolve(methodBuilder.getReturnTypeName()));
            }
        }
        return Optional.empty();
    }
}
