package com.tngtech.archunit.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Supplier;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.RawAccessRecord.TargetInfo;
import org.objectweb.asm.Type;

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
            private final ImportedClasses classes;
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
                Supplier<Optional<JavaConstructor>> constructorSupplier = new Supplier<Optional<JavaConstructor>>() {
                    @Override
                    public Optional<JavaConstructor> get() {
                        return tryFindMatchingTarget(constructors, record.target);
                    }
                };
                List<TypeDetails> paramTypes = getArgumentTypesFrom(record.target.desc);
                return new ConstructorCallTarget(targetOwner, paramTypes, constructorSupplier);
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
                Supplier<Set<JavaMethod>> methodsSupplier = new Supplier<Set<JavaMethod>>() {
                    @Override
                    public Set<JavaMethod> get() {
                        return tryFindMatchingTarget(methods, record.target).asSet();
                    }
                };
                List<TypeDetails> parameters = getArgumentTypesFrom(record.target.desc);
                TypeDetails returnType = TypeDetails.of(Type.getReturnType(record.target.desc));
                return new MethodCallTarget(targetOwner, record.target.name, parameters, returnType, methodsSupplier);
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
                Supplier<Optional<JavaField>> fieldSupplier = new Supplier<Optional<JavaField>>() {
                    @Override
                    public Optional<JavaField> get() {
                        return tryFindMatchingTarget(fields, record.target);
                    }
                };
                TypeDetails fieldType = TypeDetails.of(Type.getType(record.target.desc));
                return new FieldAccessTarget(targetOwner, record.target.name, fieldType, fieldSupplier);
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

        private static List<TypeDetails> getArgumentTypesFrom(String descriptor) {
            List<TypeDetails> paramTypes = new ArrayList<>();
            for (Type type : Type.getArgumentTypes(descriptor)) {
                paramTypes.add(TypeDetails.of(type));
            }
            return paramTypes;
        }
    }
}
