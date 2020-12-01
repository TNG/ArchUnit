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
package com.tngtech.archunit.core.importer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.ImportContext;
import com.tngtech.archunit.core.domain.InstanceofCheck;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaAnnotation.DefaultParameterVisitor;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaStaticInitializer;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.domain.ThrowsDeclaration;
import com.tngtech.archunit.core.importer.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAnnotationBuilder.ValueBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldAccessBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.TypeParametersBuilder;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;

import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeAnnotations;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeClassHierarchy;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.completeEnclosingClass;
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
        ensureClassesOfInheritanceHierarchiesArePresent();
        ensureMetaAnnotationsArePresent();
        completeClasses();
        completeAccesses();
        return createJavaClasses(classes.getDirectlyImported(), classes.getAllWithOuterClassesSortedBeforeInnerClasses(), this);
    }

    private void ensureCallTargetsArePresent() {
        for (RawAccessRecord record : importRecord.getAccessRecords()) {
            classes.ensurePresent(record.target.owner.getFullyQualifiedClassName());
        }
    }

    private void ensureClassesOfInheritanceHierarchiesArePresent() {
        for (String superClassName : importRecord.getAllSuperClassNames()) {
            resolveInheritance(superClassName, superClassStrategy);
        }

        for (String superInterfaceName : importRecord.getAllSuperInterfaceNames()) {
            resolveInheritance(superInterfaceName, interfaceStrategy);
        }
    }

    private void resolveInheritance(String currentTypeName, Function<JavaClass, Set<String>> inheritanceStrategy) {
        for (String parent : inheritanceStrategy.apply(classes.getOrResolve(currentTypeName))) {
            resolveInheritance(parent, inheritanceStrategy);
        }
    }

    private void completeClasses() {
        for (JavaClass javaClass : classes.getAllWithOuterClassesSortedBeforeInnerClasses()) {
            completeClassHierarchy(javaClass, this);
            completeEnclosingClass(javaClass, this);
            completeTypeParameters(javaClass, this);
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
            tryProcess(constructorCallRecord, AccessRecord.Factory.forConstructorCallRecord(), processedConstructorCallRecords);
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
                .build();
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

    @Override
    public Set<JavaAnnotation<?>> getAnnotationsOfType(JavaClass javaClass) {
        return memberDependenciesByTarget.getAnnotationsOfType(javaClass);
    }

    @Override
    public Set<JavaAnnotation<?>> getAnnotationsWithParameterOfType(JavaClass javaClass) {
        return memberDependenciesByTarget.getAnnotationsWithParameterOfType(javaClass);
    }

    @Override
    public Set<InstanceofCheck> getInstanceofChecksOfType(JavaClass javaClass) {
        return memberDependenciesByTarget.getInstanceofChecksOfType(javaClass);
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
    public List<JavaTypeVariable> createTypeParameters(JavaClass owner) {
        TypeParametersBuilder typeParametersBuilder = importRecord.getTypeParameterBuildersFor(owner.getName());
        return typeParametersBuilder.build(owner, classes.byTypeName());
    }

    @Override
    public Set<JavaField> createFields(JavaClass owner) {
        Set<JavaField> fields = build(importRecord.getFieldBuildersFor(owner.getName()), owner, classes.byTypeName());
        memberDependenciesByTarget.registerFields(fields);
        return fields;
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
                        return defaultValueBuilder.isPresent() ? defaultValueBuilder.get().build(method, ClassGraphCreator.this) : Optional.absent();
                    }
                });
            }
        }
        Set<JavaMethod> methods = build(methodBuilders, owner, classes.byTypeName());
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
        if (!builder.isPresent()) {
            return Optional.absent();
        }
        JavaStaticInitializer staticInitializer = builder.get().build(owner, classes.byTypeName());
        memberDependenciesByTarget.registerStaticInitializer(staticInitializer);
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
        Map<String, JavaAnnotation<OWNER>> annotations = buildAnnotations(owner, annotationBuilders, this);
        memberDependenciesByTarget.registerAnnotations(annotations.values());
        return annotations;
    }

    @Override
    public Optional<JavaClass> createEnclosingClass(JavaClass owner) {
        Optional<String> enclosingClassName = importRecord.getEnclosingClassFor(owner.getName());
        return enclosingClassName.isPresent() ?
                Optional.of(classes.getOrResolve(enclosingClassName.get())) :
                Optional.<JavaClass>absent();
    }

    @Override
    public JavaClass resolveClass(String fullyQualifiedClassName) {
        return classes.getOrResolve(fullyQualifiedClassName);
    }

    @Override
    public Optional<JavaClass> getMethodReturnType(String declaringClassName, String methodName) {
        for (DomainBuilders.JavaMethodBuilder methodBuilder : importRecord.getMethodBuildersFor(declaringClassName)) {
            if (methodBuilder.getName().equals(methodName) && methodBuilder.hasNoParameters()) {
                return Optional.of(classes.getOrResolve(methodBuilder.getReturnTypeName()));
            }
        }
        return Optional.absent();
    }

    private static class MemberDependenciesByTarget {
        private final SetMultimap<JavaClass, JavaField> fieldTypeDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, JavaMethod> methodParameterTypeDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, JavaMethod> methodReturnTypeDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, ThrowsDeclaration<JavaMethod>> methodsThrowsDeclarationDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, JavaConstructor> constructorParameterTypeDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, ThrowsDeclaration<JavaConstructor>> constructorThrowsDeclarationDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, JavaAnnotation<?>> annotationTypeDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, JavaAnnotation<?>> annotationParameterTypeDependencies = HashMultimap.create();
        private final SetMultimap<JavaClass, InstanceofCheck> instanceofCheckDependencies = HashMultimap.create();

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
                for (InstanceofCheck instanceofCheck : method.getInstanceofChecks()) {
                    instanceofCheckDependencies.put(instanceofCheck.getRawType(), instanceofCheck);
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
                for (InstanceofCheck instanceofCheck : constructor.getInstanceofChecks()) {
                    instanceofCheckDependencies.put(instanceofCheck.getRawType(), instanceofCheck);
                }
            }
        }

        void registerAnnotations(Collection<? extends JavaAnnotation<?>> annotations) {
            for (final JavaAnnotation<?> annotation : annotations) {
                annotationTypeDependencies.put(annotation.getRawType(), annotation);
                annotation.accept(new DefaultParameterVisitor() {
                    @Override
                    public void visitClass(String propertyName, JavaClass javaClass) {
                        annotationParameterTypeDependencies.put(javaClass, annotation);
                    }

                    @Override
                    public void visitEnumConstant(String propertyName, JavaEnumConstant enumConstant) {
                        annotationParameterTypeDependencies.put(enumConstant.getDeclaringClass(), annotation);
                    }

                    @Override
                    public void visitAnnotation(String propertyName, JavaAnnotation<?> memberAnnotation) {
                        annotationParameterTypeDependencies.put(memberAnnotation.getRawType(), annotation);
                        memberAnnotation.accept(this);
                    }
                });
            }
        }

        void registerStaticInitializer(JavaStaticInitializer staticInitializer) {
            for (InstanceofCheck instanceofCheck : staticInitializer.getInstanceofChecks()) {
                instanceofCheckDependencies.put(instanceofCheck.getRawType(), instanceofCheck);
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

        Set<JavaAnnotation<?>> getAnnotationsOfType(JavaClass javaClass) {
            return annotationTypeDependencies.get(javaClass);
        }

        Set<JavaAnnotation<?>> getAnnotationsWithParameterOfType(JavaClass javaClass) {
            return annotationParameterTypeDependencies.get(javaClass);
        }

        Set<InstanceofCheck> getInstanceofChecksOfType(JavaClass javaClass) {
            return instanceofCheckDependencies.get(javaClass);
        }
    }
}
