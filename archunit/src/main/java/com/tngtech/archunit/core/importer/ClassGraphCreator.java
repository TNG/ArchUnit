/*
 * Copyright 2019 TNG Technology Consulting GmbH
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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.DomainObjectCreationContext;
import com.tngtech.archunit.core.domain.ImportContext;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaStaticInitializer;
import com.tngtech.archunit.core.domain.ThrowsDeclaration;
import com.tngtech.archunit.core.importer.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldAccessBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodCallBuilder;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;

import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeClassHierarchy;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createJavaClasses;
import static com.tngtech.archunit.core.importer.DomainBuilders.BuilderWithBuildParameter.BuildFinisher.build;
import static com.tngtech.archunit.core.importer.DomainBuilders.buildAnnotations;

class ClassGraphCreator implements ImportContext {
    private final ImportedClasses classes;

    private final ClassFileImportRecord importRecord;

    private final SetMultimap<JavaCodeUnit, FieldAccessRecord> processedFieldAccessRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<MethodCallTarget>> processedMethodCallRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<ConstructorCallTarget>> processedConstructorCallRecords = HashMultimap.create();
    private final Function<JavaClass, Set<String>> superClassStrategy;
    private final Function<JavaClass, Set<String>> interfaceStrategy;
    private final MemberDependenciesByTarget memberDependenciesByTarget = new MemberDependenciesByTarget();

    ClassGraphCreator(ClassFileImportRecord importRecord, ClassResolver classResolver) {
        this.importRecord = importRecord;
        classes = new ImportedClasses(importRecord.getClasses(), classResolver);
        superClassStrategy = createSuperClassStrategy();
        interfaceStrategy = createInterfaceStrategy();
    }

    private Function<JavaClass, Set<String>> createSuperClassStrategy() {
        return new Function<JavaClass, Set<String>>() {
            @Override
            public Set<String> apply(JavaClass input) {
                return importRecord.getSuperClassFor(input.getName()).asSet();
            }
        };
    }

    private Function<JavaClass, Set<String>> createInterfaceStrategy() {
        return new Function<JavaClass, Set<String>>() {
            @Override
            public Set<String> apply(JavaClass input) {
                return importRecord.getInterfaceNamesFor(input.getName());
            }
        };
    }

    JavaClasses complete() {
        ensureCallTargetsArePresent();
        ensureClassHierarchies();
        completeMembers();
        for (RawAccessRecord.ForField fieldAccessRecord : importRecord.getRawFieldAccessRecords()) {
            tryProcess(fieldAccessRecord, AccessRecord.Factory.forFieldAccessRecord(), processedFieldAccessRecords);
        }
        for (RawAccessRecord methodCallRecord : importRecord.getRawMethodCallRecords()) {
            tryProcess(methodCallRecord, AccessRecord.Factory.forMethodCallRecord(), processedMethodCallRecords);
        }
        for (RawAccessRecord constructorCallRecord : importRecord.getRawConstructorCallRecords()) {
            tryProcess(constructorCallRecord, AccessRecord.Factory.forConstructorCallRecord(), processedConstructorCallRecords);
        }
        return createJavaClasses(classes.getDirectlyImported(), this);
    }

    private void ensureCallTargetsArePresent() {
        for (RawAccessRecord record : importRecord.getAccessRecords()) {
            classes.ensurePresent(record.target.owner.getName());
        }
    }

    private void ensureClassHierarchies() {
        ensureClassesOfHierarchyInContext();
        for (JavaClass javaClass : classes.getAll().values()) {
            completeClassHierarchy(javaClass, this);
        }
    }

    private void ensureClassesOfHierarchyInContext() {
        for (String superClassName : ImmutableSet.copyOf(importRecord.getSuperClassNamesBySubClass().values())) {
            resolveInheritance(superClassName, superClassStrategy);
        }

        for (String superInterfaceName : ImmutableSet.copyOf(importRecord.getInterfaceNamesBySubInterface().values())) {
            resolveInheritance(superInterfaceName, interfaceStrategy);
        }
    }

    private void resolveInheritance(String currentTypeName, Function<JavaClass, Set<String>> inheritanceStrategy) {
        for (String parent : inheritanceStrategy.apply(classes.getOrResolve(currentTypeName))) {
            resolveInheritance(parent, inheritanceStrategy);
        }
    }

    private void completeMembers() {
        for (JavaClass javaClass : classes.getAll().values()) {
            DomainObjectCreationContext.completeMembers(javaClass, this);
        }
    }

    private <T extends AccessRecord<?>, B extends RawAccessRecord> void tryProcess(
            B rawRecord,
            AccessRecord.Factory<B, T> factory,
            Multimap<JavaCodeUnit, T> processedAccessRecords) {

        T processed = factory.create(rawRecord, classes);
        processedAccessRecords.put(processed.getCaller(), processed);
    }

    @Override
    public Set<JavaFieldAccess> getFieldAccessesFor(JavaCodeUnit codeUnit) {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (FieldAccessRecord record : processedFieldAccessRecords.get(codeUnit)) {
            result.add(accessBuilderFrom(new JavaFieldAccessBuilder(), record)
                    .withAccessType(record.getAccessType())
                    .build());
        }
        return result.build();
    }

    @Override
    public Set<JavaMethodCall> getMethodCallsFor(JavaCodeUnit codeUnit) {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (AccessRecord<MethodCallTarget> record : processedMethodCallRecords.get(codeUnit)) {
            result.add(accessBuilderFrom(new JavaMethodCallBuilder(), record).build());
        }
        return result.build();
    }

    @Override
    public Set<JavaConstructorCall> getConstructorCallsFor(JavaCodeUnit codeUnit) {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (AccessRecord<ConstructorCallTarget> record : processedConstructorCallRecords.get(codeUnit)) {
            result.add(accessBuilderFrom(new JavaConstructorCallBuilder(), record).build());
        }
        return result.build();
    }

    @Override
    public Set<JavaField> getFieldsOfType(JavaClass javaClass) {
        return memberDependenciesByTarget.getFieldsOfType(javaClass);
    }

    @Override
    public Set<JavaMethod> getMethodsWithParameterOfType(JavaClass javaClass) {
        return memberDependenciesByTarget.getMethodsWithParameterOfType(javaClass);
    }

    @Override
    public Set<JavaMethod> getMethodsWithReturnType(JavaClass javaClass) {
        return memberDependenciesByTarget.getMethodsWithReturnType(javaClass);
    }

    @Override
    public Set<ThrowsDeclaration<JavaMethod>> getMethodThrowsDeclarationsOfType(JavaClass javaClass) {
        return memberDependenciesByTarget.getMethodThrowsDeclarationsOfType(javaClass);
    }

    @Override
    public Set<JavaConstructor> getConstructorsWithParameterOfType(JavaClass javaClass) {
        return memberDependenciesByTarget.getConstructorsWithParameterOfType(javaClass);
    }

    @Override
    public Set<ThrowsDeclaration<JavaConstructor>> getConstructorThrowsDeclarationsOfType(JavaClass javaClass) {
        return memberDependenciesByTarget.getConstructorThrowsDeclarationsOfType(javaClass);
    }

    private <T extends AccessTarget, B extends DomainBuilders.JavaAccessBuilder<T, B>>
    B accessBuilderFrom(B builder, AccessRecord<T> record) {
        return builder
                .withOrigin(record.getCaller())
                .withTarget(record.getTarget())
                .withLineNumber(record.getLineNumber());
    }

    @Override
    public Optional<JavaClass> createSuperClass(JavaClass owner) {
        Optional<String> superClassName = importRecord.getSuperClassFor(owner.getName());
        return superClassName.isPresent() ?
                Optional.of(classes.getOrResolve(superClassName.get())) :
                Optional.<JavaClass>absent();
    }

    @Override
    public Set<JavaClass> createInterfaces(JavaClass owner) {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        for (String interfaceName : importRecord.getInterfaceNamesFor(owner.getName())) {
            result.add(classes.getOrResolve(interfaceName));
        }
        return result.build();
    }

    @Override
    public Set<JavaField> createFields(JavaClass owner) {
        Set<JavaField> fields = build(importRecord.getFieldBuildersFor(owner.getName()), owner, classes.byTypeName());
        memberDependenciesByTarget.registerFields(fields);
        return fields;
    }

    @Override
    public Set<JavaMethod> createMethods(JavaClass owner) {
        Set<JavaMethod> methods = build(importRecord.getMethodBuildersFor(owner.getName()), owner, classes.byTypeName());
        memberDependenciesByTarget.registerMethods(methods);
        return methods;
    }

    @Override
    public Set<JavaConstructor> createConstructors(JavaClass owner) {
        Set<JavaConstructor> constructors = build(importRecord.getConstructorBuildersFor(owner.getName()), owner, classes.byTypeName());
        memberDependenciesByTarget.registerConstructors(constructors);
        return constructors;
    }

    @Override
    public Optional<JavaStaticInitializer> createStaticInitializer(JavaClass owner) {
        Optional<DomainBuilders.JavaStaticInitializerBuilder> builder = importRecord.getStaticInitializerBuilderFor(owner.getName());
        return builder.isPresent() ?
                Optional.of(builder.get().build(owner, classes.byTypeName())) :
                Optional.<JavaStaticInitializer>absent();
    }

    @Override
    public Map<String, JavaAnnotation> createAnnotations(JavaClass owner) {
        return buildAnnotations(importRecord.getAnnotationsFor(owner.getName()), classes.byTypeName());
    }

    @Override
    public Optional<JavaClass> createEnclosingClass(JavaClass owner) {
        Optional<String> enclosingClassName = importRecord.getEnclosingClassFor(owner.getName());
        return enclosingClassName.isPresent() ?
                Optional.of(classes.getOrResolve(enclosingClassName.get())) :
                Optional.<JavaClass>absent();
    }

    private static class MemberDependenciesByTarget {
        private final SetMultimap<JavaClass, JavaField> fieldTypeDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, JavaMethod> methodParameterTypeDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, JavaMethod> methodReturnTypeDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, ThrowsDeclaration<JavaMethod>> methodsThrowsDeclarationDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, JavaConstructor> constructorParameterTypeDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, ThrowsDeclaration<JavaConstructor>> constructorThrowsDeclarationDependencies = HashMultimap.create();

        void registerFields(Set<JavaField> fields) {
            for (JavaField field : fields) {
                fieldTypeDependencies.put(field.getRawType(), field);
            }
        }

        void registerMethods(Set<JavaMethod> methods) {
            for (JavaMethod method : methods) {
                for (JavaClass parameter : method.getRawParameterTypes()) {
                    methodParameterTypeDependencies.put(parameter, method);
                }
                methodReturnTypeDependencies.put(method.getRawReturnType(), method);
                for (ThrowsDeclaration<JavaMethod> throwsDeclaration : method.getThrowsClause()) {
                    methodsThrowsDeclarationDependencies.put(throwsDeclaration.getRawType(), throwsDeclaration);
                }
            }
        }

        void registerConstructors(Set<JavaConstructor> constructors) {
            for (JavaConstructor constructor : constructors) {
                for (JavaClass parameter : constructor.getRawParameterTypes()) {
                    constructorParameterTypeDependencies.put(parameter, constructor);
                }
                for (ThrowsDeclaration<JavaConstructor> throwsDeclaration : constructor.getThrowsClause()) {
                    constructorThrowsDeclarationDependencies.put(throwsDeclaration.getRawType(), throwsDeclaration);
                }
            }
        }

        Set<JavaField> getFieldsOfType(JavaClass javaClass) {
            return fieldTypeDependencies.get(javaClass);
        }

        Set<JavaMethod> getMethodsWithParameterOfType(JavaClass javaClass) {
            return methodParameterTypeDependencies.get(javaClass);
        }

        Set<JavaMethod> getMethodsWithReturnType(JavaClass javaClass) {
            return methodReturnTypeDependencies.get(javaClass);
        }

        Set<ThrowsDeclaration<JavaMethod>> getMethodThrowsDeclarationsOfType(JavaClass javaClass) {
            return methodsThrowsDeclarationDependencies.get(javaClass);
        }

        Set<JavaConstructor> getConstructorsWithParameterOfType(JavaClass javaClass) {
            return constructorParameterTypeDependencies.get(javaClass);
        }

        Set<ThrowsDeclaration<JavaConstructor>> getConstructorThrowsDeclarationsOfType(JavaClass javaClass) {
            return constructorThrowsDeclarationDependencies.get(javaClass);
        }
    }
}