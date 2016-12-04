package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.RawAccessRecord.TargetInfo;
import org.objectweb.asm.Type;

import static com.tngtech.archunit.core.ReflectionUtils.classForName;
import static java.util.Collections.emptySet;

interface AccessRecord<TARGET extends AccessTarget> {
    JavaCodeUnit<?, ?> getCaller();

    TARGET getTarget();

    int getLineNumber();

    interface FieldAccessRecord extends AccessRecord<FieldAccessTarget> {
        AccessType getAccessType();
    }

    class Factory {

        static AccessRecord<ConstructorCallTarget> createConstructorCallRecord(
                RawAccessRecord record, Map<String, JavaClass> classes) {
            return new RawConstructorCallRecordProcessed(record, classes);
        }

        static AccessRecord<MethodCallTarget> createMethodCallRecord(
                RawAccessRecord record, Map<String, JavaClass> classes) {
            return new RawMethodCallRecordProcessed(record, classes);
        }

        static FieldAccessRecord createFieldAccessRecord(
                RawAccessRecord record, AccessType accessType, Map<String, JavaClass> classes) {
            return new RawFieldAccessRecordProcessed(record, accessType, classes);
        }

        private static class RawConstructorCallRecordProcessed implements AccessRecord<ConstructorCallTarget> {
            private final RawAccessRecord record;
            final Map<String, JavaClass> classes;
            private final Set<JavaConstructor> constructors;

            RawConstructorCallRecordProcessed(RawAccessRecord record, Map<String, JavaClass> classes) {
                this.record = record;
                this.classes = classes;
                constructors = getJavaClass(record.target.owner.getName(), this.classes).getAllConstructors();
            }

            @Override
            public JavaCodeUnit<?, ?> getCaller() {
                return Factory.getCaller(record.caller, classes);
            }

            @Override
            public ConstructorCallTarget getTarget() {
                Optional<JavaConstructor> matchingMethod = tryFindMatchingTarget(constructors, record.target);

                JavaConstructor constructor = matchingMethod.isPresent() ? matchingMethod.get() : createConstructorFor(record.target);
                return new ConstructorCallTarget(constructor);
            }

            private JavaConstructor createConstructorFor(TargetInfo targetInfo) {
                JavaClass owner = new JavaClass.Builder().withType(TypeDetails.of(targetInfo.owner.asClass())).build();
                return createConstructor(targetInfo, owner);
            }

            private JavaConstructor createConstructor(final TargetInfo targetInfo, JavaClass owner) {
                Constructor<?> constructor = IdentifiedTarget.ofConstructor(owner.reflect(), new ReflectionUtils.Predicate<Constructor<?>>() {
                    @Override
                    public boolean apply(Constructor<?> input) {
                        return targetInfo.hasMatchingSignatureTo(input);
                    }
                }).getOrThrow("Could not determine Constructor of type %s", targetInfo.desc);
                return new JavaConstructor.Builder().withConstructor(constructor).build(owner);
            }

            public int getLineNumber() {
                return record.lineNumber;
            }
        }

        private static class RawMethodCallRecordProcessed implements AccessRecord<MethodCallTarget> {
            private final RawAccessRecord record;
            final Map<String, JavaClass> classes;
            private final Set<JavaMethod> methods;

            RawMethodCallRecordProcessed(RawAccessRecord record, Map<String, JavaClass> classes) {
                this.record = record;
                this.classes = classes;
                methods = getJavaClass(record.target.owner.getName(), this.classes).getAllMethods();
            }

            @Override
            public JavaCodeUnit<?, ?> getCaller() {
                return Factory.getCaller(record.caller, classes);
            }

            @Override
            public MethodCallTarget getTarget() {
                Optional<JavaMethod> matchingMethod = tryFindMatchingTarget(methods, record.target);

                JavaMethod method = matchingMethod.isPresent() ? matchingMethod.get() : createMethodFor(record.target);
                return new MethodCallTarget(method);
            }

            private JavaMethod createMethodFor(TargetInfo targetInfo) {
                JavaClass owner = getJavaClass(targetInfo.owner.getName(), classes);
                return createMethod(targetInfo, owner);
            }

            @SuppressWarnings("unchecked")
            private JavaMethod createMethod(final TargetInfo targetInfo, JavaClass owner) {
                MemberDescription.ForMethod member = new MethodTargetDescription(targetInfo);
                IdentifiedTarget<Method> target = IdentifiedTarget.ofMethod(owner.reflect(), new ReflectionUtils.Predicate<Method>() {
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

            public int getLineNumber() {
                return record.lineNumber;
            }
        }

        private static class RawFieldAccessRecordProcessed implements FieldAccessRecord {
            private final RawAccessRecord record;
            private final AccessType accessType;
            final Map<String, JavaClass> classes;
            private final Set<JavaField> fields;

            RawFieldAccessRecordProcessed(RawAccessRecord record, AccessType accessType, Map<String, JavaClass> classes) {
                this.record = record;
                this.accessType = accessType;
                this.classes = classes;
                fields = getJavaClass(record.target.owner.getName(), this.classes).getAllFields();
            }

            @Override
            public AccessType getAccessType() {
                return accessType;
            }

            @Override
            public JavaCodeUnit<?, ?> getCaller() {
                return Factory.getCaller(record.caller, classes);
            }

            @Override
            public FieldAccessTarget getTarget() {
                Optional<JavaField> matchingField = tryFindMatchingTarget(fields, record.target);

                JavaField field = matchingField.isPresent() ? matchingField.get() : createFieldFor(record.target);
                return new FieldAccessTarget(field);
            }

            private JavaField createFieldFor(TargetInfo targetInfo) {
                JavaClass owner = new JavaClass.Builder().withType(TypeDetails.of(targetInfo.owner.asClass())).build();
                return createField(targetInfo, owner);
            }

            @SuppressWarnings("unchecked")
            private JavaField createField(final TargetInfo targetInfo, JavaClass owner) {
                Field field = IdentifiedTarget.ofField(owner.reflect(), new ReflectionUtils.Predicate<Field>() {
                    @Override
                    public boolean apply(Field input) {
                        return targetInfo.hasMatchingSignatureTo(input);
                    }
                }).getOrThrow("Could not determine Field %s of type %s", targetInfo.name, targetInfo.desc);
                return new JavaField.Builder().withField(field).build(owner);
            }

            public int getLineNumber() {
                return record.lineNumber;
            }
        }

        private static JavaCodeUnit<?, ?> getCaller(CodeUnit caller, Map<String, JavaClass> classes) {
            for (JavaCodeUnit<?, ?> method : getJavaClass(caller.getDeclaringClassName(), classes).getCodeUnits()) {
                if (caller.is(method)) {
                    return method;
                }
            }
            throw new IllegalStateException("Never found a " + JavaCodeUnit.class.getSimpleName() +
                    " that matches supposed caller " + caller);
        }

        private static JavaClass getJavaClass(String typeName, Map<String, JavaClass> classes) {
            if (!classes.containsKey(typeName)) {
                classes.put(typeName, ImportWorkaround.resolveClass(typeName));
            }
            return classes.get(typeName);
        }

        private static <T extends HasOwner.IsOwnedByClass & HasName & HasDescriptor> Optional<T>
        tryFindMatchingTarget(Set<T> possibleTargets, TargetInfo targetInfo) {
            for (T possibleTarget : possibleTargets) {
                if (targetInfo.matches(possibleTarget)) {
                    return Optional.of(possibleTarget);
                }
            }
            return Optional.absent();
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
}
