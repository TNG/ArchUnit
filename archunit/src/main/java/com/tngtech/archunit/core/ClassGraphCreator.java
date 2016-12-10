package com.tngtech.archunit.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClassGraphCreator implements ImportContext {
    private static final Logger LOG = LoggerFactory.getLogger(ClassGraphCreator.class);

    private final ImportedClasses classes;

    private final ClassFileImportRecord importRecord;

    private final SetMultimap<JavaCodeUnit<?, ?>, FieldAccessRecord> processedFieldAccessRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit<?, ?>, AccessRecord<MethodCallTarget>> processedMethodCallRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit<?, ?>, AccessRecord<ConstructorCallTarget>> processedConstructorCallRecords = HashMultimap.create();
    private final ClassResolver classResolver;

    ClassGraphCreator(ClassFileImportRecord importRecord) {
        this.importRecord = importRecord;
        classResolver = getClassResolver();
        classes = new ImportedClasses(importRecord.getClasses(), classResolver);
    }

    JavaClasses complete() {
        ensureClassHierarchies();
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

    private void ensureClassHierarchies() {
        ensureClassesOfHierarchyInContext();
        for (JavaClass javaClass : classes.getAll().values()) {
            javaClass.completeClassHierarchyFrom(this);
        }
    }

    private void ensureClassesOfHierarchyInContext() {
        Map<String, JavaClass> missingTypes = new HashMap<>();
        for (String name : classes.getAll().keySet()) {
            tryAddSuperTypes(missingTypes, name);
        }
        classes.add(missingTypes);
    }

    private void tryAddSuperTypes(Map<String, JavaClass> missingTypes, String className) {
        try {
            for (JavaClass toAdd : classResolver.getAllSuperClasses(className)) {
                if (!classes.contain(toAdd.getName())) {
                    missingTypes.put(toAdd.getName(), toAdd);
                }
            }
        } catch (NoClassDefFoundError e) {
            LOG.warn("Can't analyse related type of '{}' because of missing dependency '{}'",
                    className, e.getMessage());
        } catch (ReflectionException e) {
            LOG.warn("Can't analyse related type of '{}' because of missing dependency. Error was: '{}'",
                    className, e.getMessage());
        }
    }

    private <T extends AccessRecord<?>, B extends RawAccessRecord> void tryProcess(
            B rawRecord, AccessRecord.Factory<B, T> factory, Multimap<JavaCodeUnit<?, ?>, T> processedAccessRecords) {
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
    public Set<FieldAccessRecord> getFieldAccessRecordsFor(JavaCodeUnit<?, ?> method) {
        return processedFieldAccessRecords.get(method);
    }

    @Override
    public Set<AccessRecord<MethodCallTarget>> getMethodCallRecordsFor(JavaCodeUnit<?, ?> method) {
        return processedMethodCallRecords.get(method);
    }

    @Override
    public Set<AccessRecord<ConstructorCallTarget>> getConstructorCallRecordsFor(JavaCodeUnit<?, ?> method) {
        return processedConstructorCallRecords.get(method);
    }

    @Override
    public Optional<JavaClass> tryGetJavaClassWithType(String typeName) {
        return Optional.of(classes.get(typeName));
    }

    private ClassResolver getClassResolver() {
        return new ClassResolverFromClassPath();
    }

    private static class ClassResolverFromClassPath implements ClassResolver {
        @Override
        public JavaClass resolve(String typeName) {
            return ImportWorkaround.resolveClass(typeName);
        }

        @Override
        public Set<JavaClass> getAllSuperClasses(String className) {
            return ImportWorkaround.getAllSuperClasses(className);
        }
    }
}
