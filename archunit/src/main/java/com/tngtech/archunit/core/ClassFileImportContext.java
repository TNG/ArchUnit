package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.tngtech.archunit.core.AccessRecord.FieldAccessRecord;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.ClassFileProcessor.CodeUnit;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.ReflectionUtils.Predicate;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.JavaClass.withType;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.ReflectionUtils.classForName;
import static java.util.Collections.emptySet;
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
            BaseRawAccessRecord<T> fieldAccessRecord, Multimap<JavaCodeUnit<?, ?>, T> processedAccessRecords) {
        try {
            T processed = fieldAccessRecord.process(classes);
            processedAccessRecords.put(processed.getCaller(), processed);
        } catch (NoClassDefFoundError e) {
            LOG.warn("Can't analyse access to '{}' because of missing dependency '{}'",
                    fieldAccessRecord.target, e.getMessage());
        } catch (ReflectionException e) {
            LOG.warn("Can't analyse access to '{}' because of missing dependency. Error was: '{}'",
                    fieldAccessRecord.target, e.getMessage());
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

    static class RawFieldAccessRecord extends BaseRawAccessRecord<FieldAccessRecord> {
        private final AccessType accessType;

        private RawFieldAccessRecord(RawFieldAccessRecord.Builder builder) {
            super(builder);
            accessType = builder.accessType;
        }

        @Override
        FieldAccessRecord process(Map<String, JavaClass> classes) {
            return new Processed(classes);
        }

        class Processed extends BaseRawAccessRecord<FieldAccessRecord>.Processed implements FieldAccessRecord {
            private final Set<JavaField> fields;

            Processed(Map<String, JavaClass> classes) {
                super(classes);
                fields = getTargetOwnerClass().getAllFields();
            }

            @Override
            public AccessType getAccessType() {
                return accessType;
            }

            @Override
            public FieldAccessTarget getTarget() {
                Optional<JavaField> matchingField = tryFindMatchingTarget(fields, target);

                JavaField field = matchingField.isPresent() ? matchingField.get() : createFieldFor(target);
                return new FieldAccessTarget(field);
            }

            private JavaField createFieldFor(TargetInfo targetInfo) {
                JavaClass owner = new JavaClass.Builder().withType(TypeDetails.of(targetInfo.owner.asClass())).build();
                return createField(targetInfo, owner);
            }

            @SuppressWarnings("unchecked")
            private JavaField createField(final TargetInfo targetInfo, JavaClass owner) {
                Field field = IdentifiedTarget.ofField(owner.reflect(), new Predicate<Field>() {
                    @Override
                    public boolean apply(Field input) {
                        return targetInfo.hasMatchingSignatureTo(input);
                    }
                }).getOrThrow("Could not determine Field %s of type %s", targetInfo.name, targetInfo.desc);
                return new JavaField.Builder().withField(field).build(owner);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{accessType=" + accessType + "," + fieldsAsString() + '}';
        }

        static class Builder extends BaseRawAccessRecord.Builder<Builder> {
            private AccessType accessType;

            Builder withAccessType(AccessType accessType) {
                this.accessType = accessType;
                return self();
            }

            RawFieldAccessRecord build() {
                return new RawFieldAccessRecord(this);
            }
        }
    }

    static class RawConstructorCallRecord extends BaseRawAccessRecord<AccessRecord<ConstructorCallTarget>> {
        private RawConstructorCallRecord(Builder builder) {
            super(builder);
        }

        @Override
        public AccessRecord<ConstructorCallTarget> process(Map<String, JavaClass> classes) {
            return new Processed(classes);
        }

        class Processed extends BaseRawAccessRecord<AccessRecord<ConstructorCallTarget>>.Processed implements AccessRecord<ConstructorCallTarget> {
            private final Set<JavaConstructor> constructors;

            Processed(Map<String, JavaClass> classes) {
                super(classes);
                constructors = getTargetOwnerClass().getAllConstructors();
            }

            @Override
            public ConstructorCallTarget getTarget() {
                Optional<JavaConstructor> matchingMethod = tryFindMatchingTarget(constructors, target);

                JavaConstructor constructor = matchingMethod.isPresent() ? matchingMethod.get() : createConstructorFor(target);
                return new ConstructorCallTarget(constructor);
            }

            private JavaConstructor createConstructorFor(TargetInfo targetInfo) {
                JavaClass owner = new JavaClass.Builder().withType(TypeDetails.of(targetInfo.owner.asClass())).build();
                return createConstructor(targetInfo, owner);
            }

            private JavaConstructor createConstructor(final TargetInfo targetInfo, JavaClass owner) {
                Constructor<?> constructor = IdentifiedTarget.ofConstructor(owner.reflect(), new Predicate<Constructor<?>>() {
                    @Override
                    public boolean apply(Constructor<?> input) {
                        return targetInfo.hasMatchingSignatureTo(input);
                    }
                }).getOrThrow("Could not determine Constructor of type %s", targetInfo.desc);
                return new JavaConstructor.Builder().withConstructor(constructor).build(owner);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + fieldsAsString() + '}';
        }

        static class Builder extends BaseRawAccessRecord.Builder<Builder> {
            RawConstructorCallRecord build() {
                return new RawConstructorCallRecord(this);
            }
        }
    }

    static class RawMethodCallRecord extends BaseRawAccessRecord<AccessRecord<MethodCallTarget>> {
        private RawMethodCallRecord(Builder builder) {
            super(builder);
        }

        @Override
        public AccessRecord<MethodCallTarget> process(Map<String, JavaClass> classes) {
            return new Processed(classes);
        }

        class Processed extends BaseRawAccessRecord<AccessRecord<MethodCallTarget>>.Processed implements AccessRecord<MethodCallTarget> {
            private final Set<JavaMethod> methods;

            Processed(Map<String, JavaClass> classes) {
                super(classes);
                methods = getTargetOwnerClass().getAllMethods();
            }

            @Override
            public MethodCallTarget getTarget() {
                Optional<JavaMethod> matchingMethod = tryFindMatchingTarget(methods, target);

                JavaMethod method = matchingMethod.isPresent() ? matchingMethod.get() : createMethodFor(target);
                return new MethodCallTarget(method);
            }

            private JavaMethod createMethodFor(TargetInfo targetInfo) {
                JavaClass owner = getJavaClass(targetInfo.owner.getName());
                return createMethod(targetInfo, owner);
            }

            @SuppressWarnings("unchecked")
            private JavaMethod createMethod(final TargetInfo targetInfo, JavaClass owner) {
                MemberDescription.ForMethod member = new MethodTargetDescription(targetInfo);
                IdentifiedTarget<Method> target = IdentifiedTarget.ofMethod(owner.reflect(), new Predicate<Method>() {
                    @Override
                    public boolean apply(Method input) {
                        return targetInfo.hasMatchingSignatureTo(input);
                    }
                });
                if (target.wasIdentified()) {
                    member = new MemberDescription.ForDeterminedMethod(target.get());
                }
                return new JavaMethod.Builder().withMember(member).build(owner);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + fieldsAsString() + '}';
        }

        static class Builder extends BaseRawAccessRecord.Builder<Builder> {
            RawMethodCallRecord build() {
                return new RawMethodCallRecord(this);
            }
        }
    }

    abstract static class BaseRawAccessRecord<PROCESSED_RECORD extends AccessRecord<?>> extends BaseAccessRecord<CodeUnit, TargetInfo> {
        private BaseRawAccessRecord(Builder<?> builder) {
            super(builder.caller, builder.target, builder.lineNumber);
        }

        abstract PROCESSED_RECORD process(Map<String, JavaClass> classes);

        abstract class Processed {
            private final Map<String, JavaClass> classes;

            Processed(Map<String, JavaClass> classes) {
                this.classes = classes;
            }

            public int getLineNumber() {
                return lineNumber;
            }

            public JavaCodeUnit<?, ?> getCaller() {
                for (JavaCodeUnit<?, ?> method : getJavaClass(caller.getDeclaringClassName()).getCodeUnits()) {
                    if (caller.is(method)) {
                        return method;
                    }
                }
                throw new IllegalStateException("Never found a " + JavaCodeUnit.class.getSimpleName() +
                        " that matches supposed caller " + caller);
            }

            JavaClass getTargetOwnerClass() {
                return getJavaClass(target.owner.getName());
            }

            JavaClass getJavaClass(String typeName) {
                if (!classes.containsKey(typeName)) {
                    classes.put(typeName, ImportWorkaround.resolveClass(typeName));
                }
                return classes.get(typeName);
            }

            <T extends HasOwner.IsOwnedByClass & HasName & HasDescriptor> Optional<T> tryFindMatchingTarget(Set<T> possibleTargets, TargetInfo targetInfo) {
                for (T possibleTarget : possibleTargets) {
                    if (targetInfo.matches(possibleTarget)) {
                        return Optional.of(possibleTarget);
                    }
                }
                return Optional.absent();
            }
        }

        static class Builder<SELF extends Builder<SELF>> {
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
        }
    }

    private static class BaseAccessRecord<CALLER, TARGET> {
        final CALLER caller;
        final TARGET target;
        final int lineNumber;

        private BaseAccessRecord(CALLER caller, TARGET target, int lineNumber) {
            this.caller = checkNotNull(caller);
            this.target = checkNotNull(target);
            this.lineNumber = lineNumber;
        }

        @Override
        public int hashCode() {
            return Objects.hash(caller, target, lineNumber);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final BaseAccessRecord<?, ?> other = (BaseAccessRecord<?, ?>) obj;
            return Objects.equals(this.caller, other.caller) &&
                    Objects.equals(this.target, other.target) &&
                    Objects.equals(this.lineNumber, other.lineNumber);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + fieldsAsString() + '}';
        }

        String fieldsAsString() {
            return "caller=" + caller + ", target=" + target + ", lineNumber=" + lineNumber;
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

        public boolean hasMatchingSignatureTo(Field field) {
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

        public boolean hasExactlyOneMatchFor(final TargetInfo target) {
            Set<JavaClass> matching = new HashSet<>();
            for (JavaClass javaClass : path) {
                if (target.signatureExistsIn(javaClass)) {
                    matching.add(javaClass);
                }
            }
            return matching.size() == 1;
        }
    }

    private static class MethodTargetDescription implements MemberDescription.ForMethod {
        private final TargetInfo targetInfo;

        private MethodTargetDescription(TargetInfo targetInfo) {
            this.targetInfo = targetInfo;
        }

        @Override
        public String getName() {
            return targetInfo.name;
        }

        // NOTE: If we can't determine the method, it must be some sort of diamond scenario, where the called target
        //       is an interface. Any interface method, by the JLS, is exactly 'public' and 'abstract',
        @Override
        public int getModifiers() {
            return Modifier.PUBLIC + Modifier.ABSTRACT;
        }

        @Override
        public Set<JavaAnnotation> getAnnotationsFor(JavaMember<?, ?> owner) {
            return emptySet();
        }

        @Override
        public String getDescriptor() {
            return targetInfo.desc;
        }

        @Override
        public Method reflect() {
            throw new ReflectionNotPossibleException(targetInfo.owner.getName(), targetInfo.name, targetInfo.desc);
        }

        @Override
        public void checkCompatibility(JavaClass owner) {
        }

        @Override
        public List<TypeDetails> getParameterTypes() {
            Type[] argumentTypes = Type.getArgumentTypes(targetInfo.desc);
            ImmutableList.Builder<TypeDetails> result = ImmutableList.builder();
            for (Type type : argumentTypes) {
                result.add(TypeDetails.of(classForName(type.getClassName())));
            }
            return result.build();
        }

        @Override
        public TypeDetails getReturnType() {
            return TypeDetails.of(classForName(Type.getReturnType(targetInfo.desc).getClassName()));
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetInfo);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final MethodTargetDescription other = (MethodTargetDescription) obj;
            return Objects.equals(this.targetInfo, other.targetInfo);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{targetInfo=" + targetInfo + '}';
        }
    }
}
