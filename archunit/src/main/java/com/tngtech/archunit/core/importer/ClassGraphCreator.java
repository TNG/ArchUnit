package com.tngtech.archunit.core.importer;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.AccessTarget;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.DomainObjectCreationContext;
import com.tngtech.archunit.core.ImportContext;
import com.tngtech.archunit.core.JavaAnnotation;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaCodeUnit;
import com.tngtech.archunit.core.JavaConstructor;
import com.tngtech.archunit.core.JavaConstructorCall;
import com.tngtech.archunit.core.JavaField;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaMethod;
import com.tngtech.archunit.core.JavaMethodCall;
import com.tngtech.archunit.core.JavaStaticInitializer;
import com.tngtech.archunit.core.importer.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorCallBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaFieldAccessBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodCallBuilder;

import static com.tngtech.archunit.core.DomainObjectCreationContext.completeClassHierarchy;
import static com.tngtech.archunit.core.DomainObjectCreationContext.createJavaClasses;
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
            resolveInheritance(parent, interfaceStrategy);
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

    private <T extends AccessTarget, B extends DomainBuilders.JavaAccessBuilder<T, B>>
    B accessBuilderFrom(B builder, AccessRecord<T> record) {
        return builder
                .withOrigin(record.getCaller())
                .withTarget(record.getTarget())
                .withLineNumber(record.getLineNumber());
    }

    @Override
    public JavaClass getJavaClassWithType(String typeName) {
        return classes.getOrResolve(typeName);
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
        return build(importRecord.getFieldBuildersFor(owner.getName()), owner, classes.byTypeName());
    }

    @Override
    public Set<JavaMethod> createMethods(JavaClass owner) {
        return build(importRecord.getMethodBuildersFor(owner.getName()), owner, classes.byTypeName());
    }

    @Override
    public Set<JavaConstructor> createConstructors(JavaClass owner) {
        return build(importRecord.getConstructorBuildersFor(owner.getName()), owner, classes.byTypeName());
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
}