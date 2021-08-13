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
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorReferenceTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodReferenceTarget;
import com.tngtech.archunit.core.domain.ImportContext;
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
import com.tngtech.archunit.core.importer.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder.ValueBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaClassTypeParametersBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorReferenceBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldAccessBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodReferenceBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaParameterizedTypeBuilder;
import com.tngtech.archunit.core.importer.ImportedClasses.MethodReturnTypeGetter;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;

import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeAnnotations;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeClassHierarchy;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeEnclosingDeclaration;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeGenericInterfaces;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeGenericSuperclass;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeMembers;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeTypeParameters;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createJavaClasses;
import static com.tngtech.archunit.core.importer.DomainBuilders.BuilderWithBuildParameter.BuildFinisher.build;
import static com.tngtech.archunit.core.importer.DomainBuilders.buildAnnotations;

class ClassGraphCreator implements ImportContext {
    private final ImportedClasses classes;

    private final ClassFileImportRecord importRecord;

    private final SetMultimap<JavaCodeUnit, FieldAccessRecord> processedFieldAccessRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<MethodCallTarget>> processedMethodCallRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<ConstructorCallTarget>> processedConstructorCallRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<MethodReferenceTarget>> processedMethodReferenceRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<ConstructorReferenceTarget>> processedConstructorReferenceRecords = HashMultimap.create();
    private final Function<JavaClass, ? extends Collection<String>> superclassStrategy;
    private final Function<JavaClass, ? extends Collection<String>> interfaceStrategy;

    ClassGraphCreator(ClassFileImportRecord importRecord, ClassResolver classResolver) {
        this.importRecord = importRecord;
        classes = new ImportedClasses(importRecord.getClasses(), classResolver, new MethodReturnTypeGetter() {
            @Override
            public Optional<JavaClass> getReturnType(String declaringClassName, String methodName) {
                return getMethodReturnType(declaringClassName, methodName);
            }
        });
        superclassStrategy = createSuperclassStrategy();
        interfaceStrategy = createInterfaceStrategy();
    }

    private Function<JavaClass, Set<String>> createSuperclassStrategy() {
        return new Function<JavaClass, Set<String>>() {
            @Override
            public Set<String> apply(JavaClass input) {
                return importRecord.getSuperclassFor(input.getName()).asSet();
            }
        };
    }

    private Function<JavaClass, List<String>> createInterfaceStrategy() {
        return new Function<JavaClass, List<String>>() {
            @Override
            public List<String> apply(JavaClass input) {
                return importRecord.getInterfaceNamesFor(input.getName());
            }
        };
    }

    JavaClasses complete() {
        ensureMemberTypesArePresent();
        ensureCallTargetsArePresent();
        ensureClassesOfInheritanceHierarchiesArePresent();
        ensureMetaAnnotationsArePresent();
        completeClasses();
        completeAccesses();
        return createJavaClasses(classes.getDirectlyImported(), classes.getAllWithOuterClassesSortedBeforeInnerClasses(), this);
    }

    private void ensureMemberTypesArePresent() {
        for (String typeName : importRecord.getMemberSignatureTypeNames()) {
            classes.ensurePresent(typeName);
        }
    }

    private void ensureCallTargetsArePresent() {
        for (RawAccessRecord record : importRecord.getAccessRecords()) {
            classes.ensurePresent(record.target.owner.getFullyQualifiedClassName());
        }
    }

    private void ensureClassesOfInheritanceHierarchiesArePresent() {
        for (String superclassName : importRecord.getAllSuperclassNames()) {
            resolveInheritance(superclassName, superclassStrategy);
        }

        for (String superinterfaceName : importRecord.getAllSuperinterfaceNames()) {
            resolveInheritance(superinterfaceName, interfaceStrategy);
        }
    }

    private void resolveInheritance(String currentTypeName, Function<JavaClass, ? extends Collection<String>> inheritanceStrategy) {
        for (String parent : inheritanceStrategy.apply(classes.getOrResolve(currentTypeName))) {
            resolveInheritance(parent, inheritanceStrategy);
        }
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

    private void completeAccesses() {
        for (RawAccessRecord.ForField fieldAccessRecord : importRecord.getRawFieldAccessRecords()) {
            tryProcess(fieldAccessRecord, AccessRecord.Factory.forFieldAccessRecord(), processedFieldAccessRecords);
        }
        for (RawAccessRecord methodCallRecord : importRecord.getRawMethodCallRecords()) {
            tryProcess(methodCallRecord, AccessRecord.Factory.forMethodCallRecord(), processedMethodCallRecords);
        }
        for (RawAccessRecord constructorCallRecord : importRecord.getRawConstructorCallRecords()) {
            tryProcess(constructorCallRecord, AccessRecord.Factory.forConstructorCallRecord(),
                    processedConstructorCallRecords);
        }
        for (RawAccessRecord methodReferenceCallRecord : importRecord.getRawMethodReferenceRecords()) {
            tryProcess(methodReferenceCallRecord, AccessRecord.Factory.forMethodReferenceRecord(),
                    processedMethodReferenceRecords);
        }
        for (RawAccessRecord constructorReferenceCallRecord : importRecord.getRawConstructorReferenceRecords()) {
            tryProcess(constructorReferenceCallRecord, AccessRecord.Factory.forConstructorReferenceRecord(),
                    processedConstructorReferenceRecords);
        }
    }

    private void ensureMetaAnnotationsArePresent() {
        for (JavaClass javaClass : classes.getAllWithOuterClassesSortedBeforeInnerClasses()) {
            resolveAnnotationHierarchy(javaClass);
        }
    }

    private void resolveAnnotationHierarchy(JavaClass javaClass) {
        for (String annotationTypeName : getAnnotationTypeNamesToResolveFor(javaClass)) {
            boolean hadBeenPreviouslyResolved = classes.isPresent(annotationTypeName);
            JavaClass annotationType = classes.getOrResolve(annotationTypeName);

            if (!hadBeenPreviouslyResolved) {
                resolveAnnotationHierarchy(annotationType);
            }
        }
    }

    private Set<String> getAnnotationTypeNamesToResolveFor(JavaClass javaClass) {
        return ImmutableSet.<String>builder()
                .addAll(importRecord.getAnnotationTypeNamesFor(javaClass))
                .addAll(importRecord.getMemberAnnotationTypeNamesFor(javaClass))
                .addAll(importRecord.getParameterAnnotationTypeNamesFor(javaClass))
                .build();
    }

    private <T extends AccessRecord<?>, B extends RawAccessRecord> void tryProcess(
            B rawRecord,
            AccessRecord.Factory<B, T> factory,
            Multimap<JavaCodeUnit, T> processedAccessRecords) {

        T processed = factory.create(rawRecord, classes);
        processedAccessRecords.put(processed.getOrigin(), processed);
    }

    @Override
    public Set<JavaFieldAccess> createFieldAccessesFor(JavaCodeUnit codeUnit) {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (FieldAccessRecord record : processedFieldAccessRecords.get(codeUnit)) {
            result.add(accessBuilderFrom(new JavaFieldAccessBuilder(), record)
                    .withAccessType(record.getAccessType())
                    .build());
        }
        return result.build();
    }

    @Override
    public Set<JavaMethodCall> createMethodCallsFor(JavaCodeUnit codeUnit) {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (AccessRecord<MethodCallTarget> record : processedMethodCallRecords.get(codeUnit)) {
            result.add(accessBuilderFrom(new JavaMethodCallBuilder(), record).build());
        }
        return result.build();
    }

    @Override
    public Set<JavaConstructorCall> createConstructorCallsFor(JavaCodeUnit codeUnit) {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (AccessRecord<ConstructorCallTarget> record : processedConstructorCallRecords.get(codeUnit)) {
            result.add(accessBuilderFrom(new JavaConstructorCallBuilder(), record).build());
        }
        return result.build();
    }

    @Override
    public Set<JavaMethodReference> createMethodReferencesFor(JavaCodeUnit codeUnit) {
        ImmutableSet.Builder<JavaMethodReference> result = ImmutableSet.builder();
        for (AccessRecord<MethodReferenceTarget> record : processedMethodReferenceRecords.get(codeUnit)) {
            result.add(accessBuilderFrom(new JavaMethodReferenceBuilder(), record).build());
        }
        return result.build();
    }

    @Override
    public Set<JavaConstructorReference> createConstructorReferencesFor(JavaCodeUnit codeUnit) {
        ImmutableSet.Builder<JavaConstructorReference> result = ImmutableSet.builder();
        for (AccessRecord<ConstructorReferenceTarget> record : processedConstructorReferenceRecords.get(codeUnit)) {
            result.add(accessBuilderFrom(new JavaConstructorReferenceBuilder(), record).build());
        }
        return result.build();
    }

    private <T extends AccessTarget, B extends DomainBuilders.JavaAccessBuilder<T, B>>
    B accessBuilderFrom(B builder, AccessRecord<T> record) {
        return builder
                .withOrigin(record.getOrigin())
                .withTarget(record.getTarget())
                .withLineNumber(record.getLineNumber());
    }

    @Override
    public Optional<JavaClass> createSuperclass(JavaClass owner) {
        Optional<String> superclassName = importRecord.getSuperclassFor(owner.getName());
        return superclassName.isPresent() ?
                Optional.of(classes.getOrResolve(superclassName.get())) :
                Optional.<JavaClass>empty();
    }

    @Override
    public Optional<JavaType> createGenericSuperclass(JavaClass owner) {
        Optional<JavaParameterizedTypeBuilder<JavaClass>> genericSuperclassBuilder = importRecord.getGenericSuperclassFor(owner);
        return genericSuperclassBuilder.isPresent()
                ? Optional.of(genericSuperclassBuilder.get().build(owner, getTypeParametersInContextOf(owner), classes))
                : Optional.<JavaType>empty();
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
        return Optional.<List<JavaType>>of(result.build());
    }

    private static Iterable<JavaTypeVariable<?>> getTypeParametersInContextOf(JavaClass javaClass) {
        Set<JavaTypeVariable<?>> result = Sets.<JavaTypeVariable<?>>newHashSet(javaClass.getTypeParameters());
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
        Set<DomainBuilders.JavaMethodBuilder> methodBuilders = importRecord.getMethodBuildersFor(owner.getName());
        if (owner.isAnnotation()) {
            for (DomainBuilders.JavaMethodBuilder methodBuilder : methodBuilders) {
                methodBuilder.withAnnotationDefaultValue(new Function<JavaMethod, Optional<Object>>() {
                    @Override
                    public Optional<Object> apply(JavaMethod method) {
                        Optional<ValueBuilder> defaultValueBuilder = importRecord.getAnnotationDefaultValueBuilderFor(method);
                        return defaultValueBuilder.isPresent() ? defaultValueBuilder.get().build(method, classes) : Optional.empty();
                    }
                });
            }
        }
        return build(methodBuilders, owner, classes);
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
        return enclosingClassName.isPresent() ?
                Optional.of(classes.getOrResolve(enclosingClassName.get())) :
                Optional.<JavaClass>empty();
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
    public JavaClass resolveClass(String fullyQualifiedClassName) {
        return classes.getOrResolve(fullyQualifiedClassName);
    }

    private Optional<JavaClass> getMethodReturnType(String declaringClassName, String methodName) {
        for (DomainBuilders.JavaMethodBuilder methodBuilder : importRecord.getMethodBuildersFor(declaringClassName)) {
            if (methodBuilder.getName().equals(methodName) && methodBuilder.hasNoParameters()) {
                return Optional.of(classes.getOrResolve(methodBuilder.getReturnTypeName()));
            }
        }
        return Optional.empty();
    }
}
