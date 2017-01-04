package com.tngtech.archunit.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.RawAccessRecord.TargetInfo;
import org.objectweb.asm.Type;

import static com.google.common.collect.Iterables.getOnlyElement;

interface AccessRecord<TARGET extends AccessTarget> {
    JavaCodeUnit getCaller();

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
            private final JavaClass targetOwner;
            private final Supplier<JavaCodeUnit> callerSupplier;

            RawConstructorCallRecordProcessed(RawAccessRecord record, ImportedClasses classes) {
                this.record = record;
                this.classes = classes;
                targetOwner = this.classes.get(record.target.owner.getName());
                callerSupplier = createCallerSupplier(record, classes);
            }

            @Override
            public JavaCodeUnit getCaller() {
                return callerSupplier.get();
            }

            @Override
            public ConstructorCallTarget getTarget() {
                Supplier<Optional<JavaConstructor>> constructorSupplier = new Supplier<Optional<JavaConstructor>>() {
                    @Override
                    public Optional<JavaConstructor> get() {
                        return uniqueTargetIn(tryFindMatchingTargets(targetOwner.getAllConstructors(), record.target));
                    }
                };
                JavaClassList paramTypes = getArgumentTypesFrom(record.target.desc, classes);
                JavaClass returnType = classes.get(void.class.getName());
                return new ConstructorCallTarget(targetOwner, paramTypes, returnType, constructorSupplier);
            }

            public int getLineNumber() {
                return record.lineNumber;
            }
        }

        private static class RawMethodCallRecordProcessed implements AccessRecord<MethodCallTarget> {
            private final RawAccessRecord record;
            final ImportedClasses classes;
            private final JavaClass targetOwner;
            private final Supplier<JavaCodeUnit> callerSupplier;

            RawMethodCallRecordProcessed(RawAccessRecord record, ImportedClasses classes) {
                this.record = record;
                this.classes = classes;
                targetOwner = this.classes.get(record.target.owner.getName());
                callerSupplier = createCallerSupplier(record, classes);
            }

            @Override
            public JavaCodeUnit getCaller() {
                return callerSupplier.get();
            }

            @Override
            public MethodCallTarget getTarget() {
                Supplier<Set<JavaMethod>> methodsSupplier = new Supplier<Set<JavaMethod>>() {
                    @Override
                    public Set<JavaMethod> get() {
                        return tryFindMatchingTargets(targetOwner.getAllMethods(), record.target);
                    }
                };
                JavaClassList parameters = getArgumentTypesFrom(record.target.desc, classes);
                JavaClass returnType = classes.get(Type.getReturnType(record.target.desc).getClassName());
                return new MethodCallTarget(targetOwner, record.target.name, parameters, returnType, methodsSupplier);
            }

            public int getLineNumber() {
                return record.lineNumber;
            }
        }

        private static class RawFieldAccessRecordProcessed implements FieldAccessRecord {
            private final RawAccessRecord.ForField record;
            final ImportedClasses classes;
            private final JavaClass targetOwner;
            private final Supplier<JavaCodeUnit> callerSupplier;

            RawFieldAccessRecordProcessed(RawAccessRecord.ForField record, ImportedClasses classes) {
                this.record = record;
                this.classes = classes;
                targetOwner = this.classes.get(record.target.owner.getName());
                callerSupplier = createCallerSupplier(record, classes);
            }

            @Override
            public AccessType getAccessType() {
                return record.accessType;
            }

            @Override
            public JavaCodeUnit getCaller() {
                return callerSupplier.get();
            }

            @Override
            public FieldAccessTarget getTarget() {
                Supplier<Optional<JavaField>> fieldSupplier = new Supplier<Optional<JavaField>>() {
                    @Override
                    public Optional<JavaField> get() {
                        return uniqueTargetIn(tryFindMatchingTargets(targetOwner.getAllFields(), record.target));
                    }
                };
                JavaClass fieldType = classes.get(Type.getType(record.target.desc).getClassName());
                return new FieldAccessTarget(targetOwner, record.target.name, fieldType, fieldSupplier);
            }

            public int getLineNumber() {
                return record.lineNumber;
            }
        }

        private static Supplier<JavaCodeUnit> createCallerSupplier(final RawAccessRecord record, final ImportedClasses classes) {
            return Suppliers.memoize(new Supplier<JavaCodeUnit>() {
                @Override
                public JavaCodeUnit get() {
                    return Factory.getCaller(record.caller, classes);
                }
            });
        }

        private static JavaCodeUnit getCaller(CodeUnit caller, ImportedClasses classes) {
            for (JavaCodeUnit method : classes.get(caller.getDeclaringClassName()).getCodeUnits()) {
                if (caller.is(method)) {
                    return method;
                }
            }
            throw new IllegalStateException("Never found a " + JavaCodeUnit.class.getSimpleName() +
                    " that matches supposed caller " + caller);
        }

        private static <T extends HasName & HasDescriptor & HasOwner<JavaClass>> Set<T>
        tryFindMatchingTargets(Set<T> possibleTargets, TargetInfo targetInfo) {
            ImmutableSet.Builder<T> result = ImmutableSet.builder();
            for (T possibleTarget : possibleTargets) {
                if (targetInfo.matches(possibleTarget)) {
                    result.add(possibleTarget);
                }
            }
            return result.build();
        }

        private static <T> Optional<T> uniqueTargetIn(Collection<T> collection) {
            return collection.size() == 1 ? Optional.of(getOnlyElement(collection)) : Optional.<T>absent();
        }

        private static JavaClassList getArgumentTypesFrom(String descriptor, ImportedClasses classes) {
            List<JavaClass> paramTypes = new ArrayList<>();
            for (Type type : Type.getArgumentTypes(descriptor)) {
                paramTypes.add(classes.get(type.getClassName()));
            }
            return new JavaClassList(paramTypes);
        }
    }
}
