package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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

    abstract class Factory<RAW_RECORD, PROCESSED_RECORD> {

        abstract PROCESSED_RECORD create(RAW_RECORD record, ImportedClasses classes);

        static Factory<RawAccessRecord, AccessRecord<ConstructorCallTarget>> forConstructorCallRecord() {
            return new Factory<RawAccessRecord, AccessRecord<ConstructorCallTarget>>() {
                @Override
                AccessRecord<ConstructorCallTarget> create(RawAccessRecord record, ImportedClasses classes) {
                    return new RawConstructorCallRecordProcessed(record, classes);
                }
            };
        }

        static Factory<RawAccessRecord, AccessRecord<MethodCallTarget>> forMethodCallRecord() {
            return new Factory<RawAccessRecord, AccessRecord<MethodCallTarget>>() {
                @Override
                AccessRecord<MethodCallTarget> create(RawAccessRecord record, ImportedClasses classes) {
                    return new RawMethodCallRecordProcessed(record, classes);
                }
            };
        }

        static Factory<RawAccessRecord.ForField, FieldAccessRecord> forFieldAccessRecord() {
            return new Factory<RawAccessRecord.ForField, FieldAccessRecord>() {
                @Override
                FieldAccessRecord create(RawAccessRecord.ForField record, ImportedClasses classes) {
                    return new RawFieldAccessRecordProcessed(record, classes);
                }
            };
        }

        private static class RawConstructorCallRecordProcessed implements AccessRecord<ConstructorCallTarget> {
            private final RawAccessRecord record;
            final ImportedClasses classes;
            private final Set<JavaConstructor> constructors;
            private final JavaClass targetOwner;

            RawConstructorCallRecordProcessed(RawAccessRecord record, ImportedClasses classes) {
                this.record = record;
                this.classes = classes;
                targetOwner = this.classes.get(record.target.owner.getName());
                constructors = targetOwner.getAllConstructors();
            }

            @Override
            public JavaCodeUnit<?, ?> getCaller() {
                return Factory.getCaller(record.caller, classes);
            }

            @Override
            public ConstructorCallTarget getTarget() {
                Optional<JavaConstructor> matchingConstructor = tryFindMatchingTarget(constructors, record.target);

                final JavaConstructor constructor = matchingConstructor.isPresent() ?
                        matchingConstructor.get() :
                        createConstructorFor(record.target);
                Supplier<Optional<JavaConstructor>> constructorSupplier = Suppliers.ofInstance(Optional.of(constructor));
                List<TypeDetails> paramTypes = getArgumentTypesFrom(record.target.desc);
                return new ConstructorCallTarget(targetOwner, paramTypes, constructorSupplier);
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
            final ImportedClasses classes;
            private final Set<JavaMethod> methods;
            private final JavaClass targetOwner;

            RawMethodCallRecordProcessed(RawAccessRecord record, ImportedClasses classes) {
                this.record = record;
                this.classes = classes;
                targetOwner = this.classes.get(record.target.owner.getName());
                methods = targetOwner.getAllMethods();
            }

            @Override
            public JavaCodeUnit<?, ?> getCaller() {
                return Factory.getCaller(record.caller, classes);
            }

            @Override
            public MethodCallTarget getTarget() {
                Optional<JavaMethod> matchingMethod = tryFindMatchingTarget(methods, record.target);

                JavaMethod method = matchingMethod.isPresent() ? matchingMethod.get() : createMethodFor(record.target);
                Supplier<Set<JavaMethod>> methodsSupplier = Suppliers.ofInstance(Collections.singleton(method));
                List<TypeDetails> parameters = getArgumentTypesFrom(record.target.desc);
                TypeDetails returnType = TypeDetails.of(Type.getReturnType(record.target.desc));
                return new MethodCallTarget(targetOwner, record.target.name, parameters, returnType, methodsSupplier);
            }

            private JavaMethod createMethodFor(TargetInfo targetInfo) {
                JavaClass owner = classes.get(targetInfo.owner.getName());
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
            private final RawAccessRecord.ForField record;
            final ImportedClasses classes;
            private final Set<JavaField> fields;
            private final JavaClass targetOwner;

            RawFieldAccessRecordProcessed(RawAccessRecord.ForField record, ImportedClasses classes) {
                this.record = record;
                this.classes = classes;
                targetOwner = this.classes.get(record.target.owner.getName());
                fields = targetOwner.getAllFields();
            }

            @Override
            public AccessType getAccessType() {
                return record.accessType;
            }

            @Override
            public JavaCodeUnit<?, ?> getCaller() {
                return Factory.getCaller(record.caller, classes);
            }

            @Override
            public FieldAccessTarget getTarget() {
                Optional<JavaField> matchingField = tryFindMatchingTarget(fields, record.target);

                JavaField field = matchingField.isPresent() ? matchingField.get() : createFieldFor(record.target);
                Supplier<Optional<JavaField>> fieldSupplier = Suppliers.ofInstance(Optional.of(field));
                TypeDetails fieldType = TypeDetails.of(Type.getType(record.target.desc));
                return new FieldAccessTarget(targetOwner, record.target.name, fieldType, fieldSupplier);
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

        private static JavaCodeUnit<?, ?> getCaller(CodeUnit caller, ImportedClasses classes) {
            for (JavaCodeUnit<?, ?> method : classes.get(caller.getDeclaringClassName()).getCodeUnits()) {
                if (caller.is(method)) {
                    return method;
                }
            }
            throw new IllegalStateException("Never found a " + JavaCodeUnit.class.getSimpleName() +
                    " that matches supposed caller " + caller);
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

        private static List<TypeDetails> getArgumentTypesFrom(String descriptor) {
            List<TypeDetails> paramTypes = new ArrayList<>();
            for (Type type : Type.getArgumentTypes(descriptor)) {
                paramTypes.add(TypeDetails.of(type));
            }
            return paramTypes;
        }
    }
}
