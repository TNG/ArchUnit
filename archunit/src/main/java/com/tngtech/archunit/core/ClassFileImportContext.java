package com.tngtech.archunit.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClassFileImportContext {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFileImportContext.class);

    private final Map<String, JavaClass> classes = new ConcurrentHashMap<>();

    private final ClassFileImportRecord importRecord;

    private final SetMultimap<JavaCodeUnit<?, ?>, FieldAccessRecord> processedFieldAccessRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit<?, ?>, AccessRecord<MethodCallTarget>> processedMethodCallRecords = HashMultimap.create();
    private final SetMultimap<JavaCodeUnit<?, ?>, AccessRecord<ConstructorCallTarget>> processedConstructorCallRecords = HashMultimap.create();

    ClassFileImportContext(ClassFileImportRecord importRecord) {
        this.importRecord = importRecord;
        for (JavaClass javaClass : importRecord.getClasses()) {
            classes.put(javaClass.getName(), javaClass);
        }
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
        return JavaClasses.of(classes, this);
    }

    private void ensureClassHierarchies() {
        ensureClassesOfHierarchyInContext();
        for (JavaClass javaClass : classes.values()) {
            javaClass.completeClassHierarchyFrom(this);
        }
    }

    private void ensureClassesOfHierarchyInContext() {
        Map<String, JavaClass> missingTypes = new HashMap<>();
        for (String name : classes.keySet()) {
            tryAddSuperTypes(missingTypes, name);
        }
        classes.putAll(missingTypes);
    }

    private void tryAddSuperTypes(Map<String, JavaClass> missingTypes, String className) {
        try {
            for (JavaClass toAdd : ImportWorkaround.getAllSuperClasses(className)) {
                if (!classes.containsKey(toAdd.getName())) {
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

    Set<FieldAccessRecord> getFieldAccessRecordsFor(JavaCodeUnit<?, ?> method) {
        return processedFieldAccessRecords.get(method);
    }

    Set<AccessRecord<MethodCallTarget>> getMethodCallRecordsFor(JavaCodeUnit<?, ?> method) {
        return processedMethodCallRecords.get(method);
    }

    Set<AccessRecord<ConstructorCallTarget>> getConstructorCallRecordsFor(JavaCodeUnit<?, ?> method) {
        return processedConstructorCallRecords.get(method);
    }

    Optional<JavaClass> tryGetJavaClassWithType(String typeName) {
        return Optional.fromNullable(classes.get(typeName));
    }
}
