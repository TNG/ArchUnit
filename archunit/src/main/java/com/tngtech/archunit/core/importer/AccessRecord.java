/*
 * Copyright 2018 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.core.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassList;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasDescriptor;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.importer.DomainBuilders.ConstructorCallTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.FieldAccessTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.MethodCallTargetBuilder;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.importer.RawAccessRecord.TargetInfo;
import org.objectweb.asm.Type;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.DomainObjectCreationContext.createJavaClassList;

interface AccessRecord<TARGET extends AccessTarget> {
    JavaCodeUnit getCaller();

    TARGET getTarget();

    int getLineNumber();

    @Internal
    interface FieldAccessRecord extends AccessRecord<FieldAccessTarget> {
        AccessType getAccessType();
    }

    @Internal
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
                targetOwner = this.classes.getOrResolve(record.target.owner.getName());
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
                JavaClass returnType = classes.getOrResolve(void.class.getName());
                return new ConstructorCallTargetBuilder()
                        .withOwner(targetOwner)
                        .withParameters(paramTypes)
                        .withReturnType(returnType)
                        .withConstructor(constructorSupplier)
                        .build();
            }

            @Override
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
                targetOwner = this.classes.getOrResolve(record.target.owner.getName());
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
                JavaClass returnType = classes.getOrResolve(Type.getReturnType(record.target.desc).getClassName());
                return new MethodCallTargetBuilder()
                        .withOwner(targetOwner)
                        .withName(record.target.name)
                        .withParameters(parameters)
                        .withReturnType(returnType)
                        .withMethods(methodsSupplier)
                        .build();
            }

            @Override
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
                targetOwner = this.classes.getOrResolve(record.target.owner.getName());
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
                JavaClass fieldType = classes.getOrResolve(Type.getType(record.target.desc).getClassName());
                return new FieldAccessTargetBuilder()
                        .withOwner(targetOwner)
                        .withName(record.target.name)
                        .withType(fieldType)
                        .withField(fieldSupplier)
                        .build();
            }

            @Override
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
            for (JavaCodeUnit method : classes.getOrResolve(caller.getDeclaringClassName()).getCodeUnits()) {
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
                paramTypes.add(classes.getOrResolve(type.getClassName()));
            }
            return createJavaClassList(paramTypes);
        }
    }
}
