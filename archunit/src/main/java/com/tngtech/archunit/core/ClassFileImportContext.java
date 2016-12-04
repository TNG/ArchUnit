package com.tngtech.archunit.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClassFileImportContext {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFileImportContext.class);

    private final Map<String, JavaClass> classes = new ConcurrentHashMap<>();

    private final Set<RawFieldAccessRecord> rawFieldAccessRecords = new HashSet<>();
    private final SetMultimap<JavaCodeUnit<?, ?>, FieldAccessRecord> processedFieldAccessRecords = HashMultimap.create();
    private final Set<RawMethodCallRecord> rawMethodCallRecords = new HashSet<>();
    private final SetMultimap<JavaCodeUnit<?, ?>, AccessRecord<MethodCallTarget>> processedMethodCallRecords = HashMultimap.create();
    private final Set<RawConstructorCallRecord> rawConstructorCallRecords = new HashSet<>();
    private final SetMultimap<JavaCodeUnit<?, ?>, AccessRecord<ConstructorCallTarget>> processedConstructorCallRecords = HashMultimap.create();

    void registerFieldAccess(RawFieldAccessRecord record) {
        rawFieldAccessRecords.add(record);
    }

    void registerMethodCall(RawMethodCallRecord record) {
        rawMethodCallRecords.add(record);
    }

    void registerConstructorCall(RawConstructorCallRecord record) {
        rawConstructorCallRecords.add(record);
    }

    JavaClasses complete() {
        ensureClassHierarchies();
        for (RawFieldAccessRecord fieldAccessRecord : rawFieldAccessRecords) {
            tryProcess(fieldAccessRecord.raw(), AccessRecord.Factory.forFieldAccessRecord(), processedFieldAccessRecords);
        }
        for (RawMethodCallRecord methodCallRecord : rawMethodCallRecords) {
            tryProcess(methodCallRecord.raw(), AccessRecord.Factory.forMethodCallRecord(), processedMethodCallRecords);
        }
        for (RawConstructorCallRecord constructorCallRecord : rawConstructorCallRecords) {
            tryProcess(constructorCallRecord.raw(), AccessRecord.Factory.forConstructorCallRecord(), processedConstructorCallRecords);
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

    void add(JavaClass javaClass) {
        classes.put(javaClass.getName(), javaClass);
    }

    Optional<JavaClass> tryGetJavaClassWithType(String typeName) {
        return Optional.fromNullable(classes.get(typeName));
    }

    static class RawFieldAccessRecord {
        final RawAccessRecord.ForField record;

        private RawFieldAccessRecord(RawAccessRecord.ForField.Builder builder) {
            this.record = builder.build();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{accessType=" + record.accessType + "," + record.fieldsAsString() + '}';
        }

        public RawAccessRecord.ForField raw() {
            return new RawAccessRecord.ForField.Builder()
                    .withCaller(record.caller)
                    .withTarget(record.target)
                    .withLineNumber(record.lineNumber)
                    .withAccessType(record.accessType)
                    .build();
        }

        static Builder builder() {
            return new Builder();
        }

        static class Builder extends RawAccessRecord.Builder<Builder> {
            private AccessType accessType;

            private Builder() {
            }

            Builder withAccessType(AccessType accessType) {
                this.accessType = accessType;
                return self();
            }

            RawFieldAccessRecord build() {
                RawAccessRecord record = buildAccessRecord();
                return new RawFieldAccessRecord(new RawAccessRecord.ForField.Builder()
                        .withCaller(record.caller)
                        .withTarget(record.target)
                        .withLineNumber(record.lineNumber)
                        .withAccessType(accessType));
            }
        }
    }

    static class RawConstructorCallRecord implements ToProcess<AccessRecord<ConstructorCallTarget>> {
        final RawAccessRecord record;

        private RawConstructorCallRecord(Builder builder) {
            this.record = builder.buildAccessRecord();
        }

        @Override
        public AccessRecord<ConstructorCallTarget> process(Map<String, JavaClass> classes) {
            return AccessRecord.Factory.forConstructorCallRecord().create(record, classes);
        }

        @Override
        public String getTarget() {
            return "" + record.target;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + record.fieldsAsString() + '}';
        }

        static Builder builder() {
            return new Builder();
        }

        public RawAccessRecord raw() {
            return new RawAccessRecord.Builder<>()
                    .withCaller(record.caller)
                    .withTarget(record.target)
                    .withLineNumber(record.lineNumber)
                    .buildAccessRecord();
        }

        static class Builder extends RawAccessRecord.Builder<Builder> {
            private Builder() {
            }

            RawConstructorCallRecord build() {
                return new RawConstructorCallRecord(this);
            }
        }
    }

    static class RawMethodCallRecord implements ToProcess<AccessRecord<MethodCallTarget>> {
        final RawAccessRecord record;

        private RawMethodCallRecord(RawAccessRecord.Builder<?> builder) {
            this.record = builder.buildAccessRecord();
        }

        @Override
        public AccessRecord<MethodCallTarget> process(Map<String, JavaClass> classes) {
            return AccessRecord.Factory.forMethodCallRecord().create(record, classes);
        }

        @Override
        public String getTarget() {
            return "" + record.target;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + record.fieldsAsString() + '}';
        }

        static Builder builder() {
            return new Builder();
        }

        public RawAccessRecord raw() {
            return new RawAccessRecord.Builder<>()
                    .withCaller(record.caller)
                    .withTarget(record.target)
                    .withLineNumber(record.lineNumber)
                    .buildAccessRecord();
        }

        static class Builder extends RawAccessRecord.Builder<Builder> {
            private Builder() {
            }

            RawMethodCallRecord build() {
                return new RawMethodCallRecord(this);
            }
        }
    }

    private interface ToProcess<PROCESSED_RECORD> {
        PROCESSED_RECORD process(Map<String, JavaClass> classes);

        String getTarget();
    }
}
