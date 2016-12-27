package com.tngtech.archunit.core;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.ArchUnitException.ReflectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.BuilderWithBuildParameter.BuildFinisher.build;
import static com.tngtech.archunit.core.ImportedClasses.simpleClassOf;
import static com.tngtech.archunit.core.JavaAnnotation.buildAnnotations;
import static com.tngtech.archunit.core.ReflectionUtils.ensureCorrectArrayTypeName;

class ClassGraphCreator implements ImportContext {
    private static final Logger LOG = LoggerFactory.getLogger(ClassGraphCreator.class);

    private final ImportedClasses classes;

    private final ClassFileImportRecord importRecord;

    private final SetMultimap<JavaCodeUnit, FieldAccessRecord> processedFieldAccessRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<MethodCallTarget>> processedMethodCallRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit, AccessRecord<ConstructorCallTarget>> processedConstructorCallRecords = HashMultimap.create();
    private final ClassResolver classResolver;

    ClassGraphCreator(ClassFileImportRecord importRecord, ClassResolver classResolver) {
        this.importRecord = importRecord;
        this.classResolver = classResolver;
        classes = new ImportedClasses(importRecord.getClasses(), classResolver);
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
        return JavaClasses.of(classes.getDirectlyImported(), this);
    }

    private void ensureCallTargetsArePresent() {
        for (RawAccessRecord record : importRecord.getAccessRecords()) {
            classes.ensurePresent(ensureCorrectArrayTypeName(record.target.owner.getName()));
        }
    }

    private void ensureClassHierarchies() {
        ensureClassesOfHierarchyInContext();
        for (JavaClass javaClass : classes.getAll().values()) {
            javaClass.completeClassHierarchyFrom(this);
        }
    }

    private void ensureClassesOfHierarchyInContext() {
        for (String name : ImmutableSet.copyOf(classes.getAll().keySet())) {
            resolveSuperTypesOf(name);
        }
    }

    private void resolveSuperTypesOf(String className) {
        for (Map.Entry<String, Optional<JavaClass>> toAdd : classResolver.getAllSuperClasses(className, classes.byType()).entrySet()) {
            if (!classes.contain(toAdd.getKey())) {
                classes.add(toAdd.getValue().or(simpleClassOf(toAdd.getKey())));
            }
        }
    }

    private void completeMembers() {
        for (JavaClass javaClass : classes.getAll().values()) {
            javaClass.completeMembers(this);
        }
    }

    private <T extends AccessRecord<?>, B extends RawAccessRecord> void tryProcess(
            B rawRecord,
            AccessRecord.Factory<B, T> factory,
            Multimap<JavaCodeUnit, T> processedAccessRecords) {
        try {
            T processed = factory.create(rawRecord, classes);
            processedAccessRecords.put(processed.getCaller(), processed);
        } catch (NoClassDefFoundError e) {
            LOG.warn("Can't analyse access to '{}' because of missing dependency '{}'",
                    rawRecord.target, e.getMessage());
        } catch (ReflectionException e) {
            LOG.warn("Can't analyse access to '{}' because of missing dependency. Error was: '{}'",
                    rawRecord.target, e.getMessage());
        }
    }

    @Override
    public Set<FieldAccessRecord> getFieldAccessRecordsFor(JavaCodeUnit method) {
        return processedFieldAccessRecords.get(method);
    }

    @Override
    public Set<AccessRecord<MethodCallTarget>> getMethodCallRecordsFor(JavaCodeUnit method) {
        return processedMethodCallRecords.get(method);
    }

    @Override
    public Set<AccessRecord<ConstructorCallTarget>> getConstructorCallRecordsFor(JavaCodeUnit method) {
        return processedConstructorCallRecords.get(method);
    }

    @Override
    public JavaClass getJavaClassWithType(String typeName) {
        return classes.get(typeName);
    }

    @Override
    public Optional<JavaClass> createSuperClass(JavaClass owner) {
        Optional<String> superClassName = importRecord.getSuperClassFor(owner.getName());
        return superClassName.isPresent() ?
                Optional.of(classes.get(superClassName.get())) :
                Optional.<JavaClass>absent();
    }

    @Override
    public Set<JavaClass> createInterfaces(JavaClass owner) {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        for (String interfaceName : importRecord.getInterfaceNamesFor(owner.getName())) {
            result.add(classes.get(interfaceName));
        }
        return result.build();
    }

    @Override
    public Set<JavaField> createFields(JavaClass owner) {
        return build(importRecord.getFieldBuildersFor(owner.getName()), owner, classes.byType());
    }

    @Override
    public Set<JavaMethod> createMethods(JavaClass owner) {
        return build(importRecord.getMethodBuildersFor(owner.getName()), owner, classes.byType());
    }

    @Override
    public Set<JavaConstructor> createConstructors(JavaClass owner) {
        return build(importRecord.getConstructorBuildersFor(owner.getName()), owner, classes.byType());
    }

    @Override
    public Optional<JavaStaticInitializer> createStaticInitializer(JavaClass owner) {
        Optional<JavaStaticInitializer.Builder> builder = importRecord.getStaticInitializerBuilderFor(owner.getName());
        return builder.isPresent() ?
                Optional.of(builder.get().build(owner, classes.byType())) :
                Optional.<JavaStaticInitializer>absent();
    }

    @Override
    public Map<String, JavaAnnotation> createAnnotations(JavaClass owner) {
        return buildAnnotations(importRecord.getAnnotationsFor(owner.getName()), classes.byType());
    }

    @Override
    public Optional<JavaClass> createEnclosingClass(JavaClass owner) {
        Optional<String> enclosingClassName = importRecord.getEnclosingClassFor(owner.getName());
        return enclosingClassName.isPresent() ?
                Optional.of(classes.get(enclosingClassName.get())) :
                Optional.<JavaClass>absent();
    }
}