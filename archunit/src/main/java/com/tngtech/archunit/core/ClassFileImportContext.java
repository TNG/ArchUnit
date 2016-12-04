package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.ClassFileProcessor.CodeUnit;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.core.JavaClass.withType;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static java.util.Collections.singleton;

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
            tryProcess(fieldAccessRecord, processedFieldAccessRecords);
        }
        for (RawMethodCallRecord methodCallRecord : rawMethodCallRecords) {
            tryProcess(methodCallRecord, processedMethodCallRecords);
        }
        for (RawConstructorCallRecord methodCallRecord : rawConstructorCallRecords) {
            tryProcess(methodCallRecord, processedConstructorCallRecords);
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

    private <T extends AccessRecord<?>> void tryProcess(
            ToProcess<T> fieldAccessRecord, Multimap<JavaCodeUnit<?, ?>, T> processedAccessRecords) {
        try {
            T processed = fieldAccessRecord.process(classes);
            processedAccessRecords.put(processed.getCaller(), processed);
        } catch (NoClassDefFoundError e) {
            LOG.warn("Can't analyse access to '{}' because of missing dependency '{}'",
                    fieldAccessRecord.getTarget(), e.getMessage());
        } catch (ReflectionException e) {
            LOG.warn("Can't analyse access to '{}' because of missing dependency. Error was: '{}'",
                    fieldAccessRecord.getTarget(), e.getMessage());
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

    static class RawFieldAccessRecord implements ToProcess<FieldAccessRecord> {
        private final RawAccessRecord record;
        private final AccessType accessType;

        private RawFieldAccessRecord(RawFieldAccessRecord.Builder builder) {
            this.record = builder.buildAccessRecord();
            accessType = builder.accessType;
        }

        @Override
        public FieldAccessRecord process(Map<String, JavaClass> classes) {
            return AccessRecord.Factory.createFieldAccessRecord(record, accessType, classes);
        }

        @Override
        public String getTarget() {
            return "" + record.target;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{accessType=" + accessType + "," + record.fieldsAsString() + '}';
        }

        static Builder builder() {
            return new Builder();
        }

        static class Builder extends BaseRawAccessRecordBuilder<Builder> {
            private AccessType accessType;

            private Builder() {
            }

            Builder withAccessType(AccessType accessType) {
                this.accessType = accessType;
                return self();
            }

            RawFieldAccessRecord build() {
                return new RawFieldAccessRecord(this);
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
            return AccessRecord.Factory.createConstructorCallRecord(record, classes);
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

        static class Builder extends BaseRawAccessRecordBuilder<Builder> {
            private Builder() {
            }

            RawConstructorCallRecord build() {
                return new RawConstructorCallRecord(this);
            }
        }
    }

    static class RawMethodCallRecord implements ToProcess<AccessRecord<MethodCallTarget>> {
        final RawAccessRecord record;

        private RawMethodCallRecord(BaseRawAccessRecordBuilder<?> builder) {
            this.record = builder.buildAccessRecord();
        }

        @Override
        public AccessRecord<MethodCallTarget> process(Map<String, JavaClass> classes) {
            return AccessRecord.Factory.createMethodCallRecord(record, classes);
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

        static class Builder extends BaseRawAccessRecordBuilder<Builder> {
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

    static class BaseRawAccessRecordBuilder<SELF extends BaseRawAccessRecordBuilder<SELF>> {
        private CodeUnit caller;
        private TargetInfo target;
        private int lineNumber = -1;

        SELF withCaller(CodeUnit caller) {
            this.caller = caller;
            return self();
        }

        SELF withTarget(TargetInfo target) {
            this.target = target;
            return self();
        }

        SELF withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return self();
        }

        @SuppressWarnings("unchecked")
        SELF self() {
            return (SELF) this;
        }

        RawAccessRecord buildAccessRecord() {
            return new RawAccessRecord(caller, target, lineNumber);
        }
    }

    static class FieldTargetInfo extends TargetInfo {
        FieldTargetInfo(String owner, String name, String desc) {
            super(owner, name, desc);
        }

        @Override
        protected boolean signatureExistsIn(JavaClass javaClass) {
            Optional<JavaField> field = javaClass.tryGetField(name);
            return field.isPresent() && desc.equals(field.get().getDescriptor());
        }
    }

    static class ConstructorTargetInfo extends TargetInfo {
        ConstructorTargetInfo(String owner, String name, String desc) {
            super(owner, name, desc);
        }

        @Override
        protected boolean signatureExistsIn(JavaClass javaClass) {
            for (JavaConstructor constructor : javaClass.getConstructors()) {
                if (hasMatchingSignatureTo(constructor.reflect())) {
                    return true;
                }
            }
            return false;
        }
    }

    static class MethodTargetInfo extends TargetInfo {
        MethodTargetInfo(String owner, String name, String desc) {
            super(owner, name, desc);
        }

        @Override
        protected boolean signatureExistsIn(JavaClass javaClass) {
            for (JavaMethod method : javaClass.getMethods()) {
                if (hasMatchingSignatureTo(method.reflect())) {
                    return true;
                }
            }
            return false;
        }
    }

    static abstract class TargetInfo {
        final JavaType owner;
        final String name;
        final String desc;

        TargetInfo(String owner, String name, String desc) {
            this.owner = JavaType.fromDescriptor(owner);
            this.name = name;
            this.desc = desc;
        }

        <T extends HasOwner.IsOwnedByClass & HasName & HasDescriptor> boolean matches(T member) {
            if (!name.equals(member.getName()) || !desc.equals(member.getDescriptor())) {
                return false;
            }
            return owner.getName().equals(member.getOwner().getName()) ||
                    classHierarchyFrom(member).hasExactlyOneMatchFor(this);
        }

        private <T extends HasOwner.IsOwnedByClass & HasName & HasDescriptor> ClassHierarchyPath classHierarchyFrom(T member) {
            return new ClassHierarchyPath(owner, member.getOwner());
        }

        protected abstract boolean signatureExistsIn(JavaClass javaClass);

        boolean hasMatchingSignatureTo(Method method) {
            return method.getName().equals(name) &&
                    Type.getMethodDescriptor(method).equals(desc);
        }

        boolean hasMatchingSignatureTo(Constructor<?> constructor) {
            return CONSTRUCTOR_NAME.equals(name) &&
                    Type.getConstructorDescriptor(constructor).equals(desc);
        }

        boolean hasMatchingSignatureTo(Field field) {
            return field.getName().equals(name) &&
                    Type.getDescriptor(field.getType()).equals(desc);
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, name, desc);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final TargetInfo other = (TargetInfo) obj;
            return Objects.equals(this.owner, other.owner) &&
                    Objects.equals(this.name, other.name) &&
                    Objects.equals(this.desc, other.desc);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{owner='" + owner.getName() + "', name='" + name + "', desc='" + desc + "'}";
        }
    }

    private static class ClassHierarchyPath {
        private final List<JavaClass> path = new ArrayList<>();

        private ClassHierarchyPath(JavaType childType, JavaClass parent) {
            Set<JavaClass> classesToSearchForChild = Sets.union(singleton(parent), parent.getAllSubClasses());
            Optional<JavaClass> child = tryFind(classesToSearchForChild, withType(childType.asClass()));
            if (child.isPresent()) {
                createPath(child.get(), parent);
            }
        }

        private static <T> Optional<T> tryFind(Iterable<T> collection, DescribedPredicate<T> predicate) {
            for (T elem : collection) {
                if (predicate.apply(elem)) {
                    return Optional.of(elem);
                }
            }
            return Optional.absent();
        }

        private void createPath(JavaClass child, JavaClass parent) {
            path.add(child);
            while (child != parent) {
                child = child.getSuperClass().get();
                path.add(child);
            }
        }

        boolean hasExactlyOneMatchFor(final TargetInfo target) {
            Set<JavaClass> matching = new HashSet<>();
            for (JavaClass javaClass : path) {
                if (target.signatureExistsIn(javaClass)) {
                    matching.add(javaClass);
                }
            }
            return matching.size() == 1;
        }
    }
}
