/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorReferenceTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodReferenceTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.DomainBuilders.CodeUnitAccessTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.FieldAccessTargetBuilder;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.importer.RawAccessRecord.TargetInfo;

import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;
import static com.tngtech.archunit.core.importer.DomainBuilders.newConstructorCallTargetBuilder;
import static com.tngtech.archunit.core.importer.DomainBuilders.newConstructorReferenceTargetBuilder;
import static com.tngtech.archunit.core.importer.DomainBuilders.newMethodCallTargetBuilder;
import static com.tngtech.archunit.core.importer.DomainBuilders.newMethodReferenceTargetBuilder;

interface AccessRecord<TARGET extends AccessTarget> {
    JavaCodeUnit getOrigin();

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
                    return new RawAccessRecordProcessed<>(record, classes, CONSTRUCTOR_CALL_TARGET_FACTORY);
                }
            };
        }

        static Factory<RawAccessRecord, AccessRecord<ConstructorReferenceTarget>> forConstructorReferenceRecord() {
            return new Factory<RawAccessRecord, AccessRecord<ConstructorReferenceTarget>>() {
                @Override
                AccessRecord<ConstructorReferenceTarget> create(RawAccessRecord record, ImportedClasses classes) {
                    return new RawAccessRecordProcessed<>(record, classes, CONSTRUCTOR_REFERENCE_TARGET_FACTORY);
                }
            };
        }

        static Factory<RawAccessRecord, AccessRecord<MethodCallTarget>> forMethodCallRecord() {
            return new Factory<RawAccessRecord, AccessRecord<MethodCallTarget>>() {
                @Override
                AccessRecord<MethodCallTarget> create(RawAccessRecord record, ImportedClasses classes) {
                    return new RawAccessRecordProcessed<>(record, classes, METHOD_CALL_TARGET_FACTORY);
                }
            };
        }

        static Factory<RawAccessRecord, AccessRecord<MethodReferenceTarget>> forMethodReferenceRecord() {
            return new Factory<RawAccessRecord, AccessRecord<MethodReferenceTarget>>() {
                @Override
                AccessRecord<MethodReferenceTarget> create(RawAccessRecord record, ImportedClasses classes) {
                    return new RawAccessRecordProcessed<>(record, classes, METHOD_REFERENCE_TARGET_FACTORY);
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

        private static final Supplier<CodeUnitAccessTargetBuilder<JavaConstructor, ConstructorCallTarget>> CONSTRUCTOR_CALL_TARGET_BUILDER_SUPPLIER =
                new Supplier<CodeUnitAccessTargetBuilder<JavaConstructor, ConstructorCallTarget>>() {
                    @Override
                    public CodeUnitAccessTargetBuilder<JavaConstructor, ConstructorCallTarget> get() {
                        return newConstructorCallTargetBuilder();
                    }
                };

        private static final Supplier<CodeUnitAccessTargetBuilder<JavaConstructor, ConstructorReferenceTarget>> CONSTRUCTOR_REFERENCE_TARGET_BUILDER_SUPPLIER =
                new Supplier<CodeUnitAccessTargetBuilder<JavaConstructor, ConstructorReferenceTarget>>() {
                    @Override
                    public CodeUnitAccessTargetBuilder<JavaConstructor, ConstructorReferenceTarget> get() {
                        return newConstructorReferenceTargetBuilder();
                    }
                };

        private static final Supplier<CodeUnitAccessTargetBuilder<JavaMethod, MethodCallTarget>> METHOD_CALL_TARGET_BUILDER_SUPPLIER =
                new Supplier<CodeUnitAccessTargetBuilder<JavaMethod, MethodCallTarget>>() {
                    @Override
                    public CodeUnitAccessTargetBuilder<JavaMethod, MethodCallTarget> get() {
                        return newMethodCallTargetBuilder();
                    }
                };

        private static final Supplier<CodeUnitAccessTargetBuilder<JavaMethod, MethodReferenceTarget>> METHOD_REFERENCE_TARGET_BUILDER_SUPPLIER =
                new Supplier<CodeUnitAccessTargetBuilder<JavaMethod, MethodReferenceTarget>>() {
                    @Override
                    public CodeUnitAccessTargetBuilder<JavaMethod, MethodReferenceTarget> get() {
                        return newMethodReferenceTargetBuilder();
                    }
                };

        private static final AccessTargetFactory<ConstructorCallTarget> CONSTRUCTOR_CALL_TARGET_FACTORY = new ConstructorAccessTargetFactory<>(CONSTRUCTOR_CALL_TARGET_BUILDER_SUPPLIER);
        private static final AccessTargetFactory<ConstructorReferenceTarget> CONSTRUCTOR_REFERENCE_TARGET_FACTORY = new ConstructorAccessTargetFactory<>(CONSTRUCTOR_REFERENCE_TARGET_BUILDER_SUPPLIER);
        private static final AccessTargetFactory<MethodCallTarget> METHOD_CALL_TARGET_FACTORY = new MethodAccessTargetFactory<>(METHOD_CALL_TARGET_BUILDER_SUPPLIER);
        private static final AccessTargetFactory<MethodReferenceTarget> METHOD_REFERENCE_TARGET_FACTORY = new MethodAccessTargetFactory<>(METHOD_REFERENCE_TARGET_BUILDER_SUPPLIER);
        private static final AccessTargetFactory<FieldAccessTarget> FIELD_ACCESS_TARGET_FACTORY = new FieldAccessTargetFactory();

        private interface AccessTargetFactory<TARGET extends AccessTarget> {
            TARGET create(JavaClass targetOwner, TargetInfo targetInfo, ImportedClasses classes);
        }

        private static class ConstructorAccessTargetFactory<TARGET extends CodeUnitAccessTarget> implements AccessTargetFactory<TARGET> {
            private final Supplier<CodeUnitAccessTargetBuilder<JavaConstructor, TARGET>> targetBuilderSupplier;

            private ConstructorAccessTargetFactory(Supplier<CodeUnitAccessTargetBuilder<JavaConstructor, TARGET>> targetBuilderSupplier) {
                this.targetBuilderSupplier = targetBuilderSupplier;
            }

            @Override
            public TARGET create(JavaClass targetOwner, TargetInfo target, ImportedClasses classes) {
                Supplier<Optional<JavaConstructor>> memberSupplier = new ConstructorSupplier(targetOwner, target);
                List<JavaClass> paramTypes = getArgumentTypesFrom(target.desc, classes);
                JavaClass returnType = classes.getOrResolve(void.class.getName());
                return targetBuilderSupplier.get()
                        .withOwner(targetOwner)
                        .withParameters(paramTypes)
                        .withReturnType(returnType)
                        .withMember(memberSupplier)
                        .build();
            }

            private static class ConstructorSupplier implements Supplier<Optional<JavaConstructor>> {
                private final JavaClass targetOwner;
                private final TargetInfo target;

                ConstructorSupplier(JavaClass targetOwner, TargetInfo target) {
                    this.targetOwner = targetOwner;
                    this.target = target;
                }

                @Override
                public Optional<JavaConstructor> get() {
                    for (JavaConstructor constructor : targetOwner.getConstructors()) {
                        if (constructor.getDescriptor().equals(target.desc)) {
                            return Optional.of(constructor);
                        }
                    }
                    return Optional.empty();
                }
            }
        }

        private static class MethodAccessTargetFactory<TARGET extends CodeUnitAccessTarget> implements AccessTargetFactory<TARGET> {
            private final Supplier<CodeUnitAccessTargetBuilder<JavaMethod, TARGET>> targetBuilderSupplier;

            private MethodAccessTargetFactory(Supplier<CodeUnitAccessTargetBuilder<JavaMethod, TARGET>> targetBuilderSupplier) {
                this.targetBuilderSupplier = targetBuilderSupplier;
            }

            @Override
            public TARGET create(JavaClass targetOwner, TargetInfo target, ImportedClasses classes) {
                Supplier<Optional<JavaMethod>> methodsSupplier = new MethodSupplier(targetOwner, target);
                List<JavaClass> parameters = getArgumentTypesFrom(target.desc, classes);
                JavaClass returnType = classes.getOrResolve(JavaClassDescriptorImporter.importAsmMethodReturnType(target.desc).getFullyQualifiedClassName());
                return targetBuilderSupplier.get()
                        .withOwner(targetOwner)
                        .withName(target.name)
                        .withParameters(parameters)
                        .withReturnType(returnType)
                        .withMember(methodsSupplier)
                        .build();
            }

            private static class MethodSupplier implements Supplier<Optional<JavaMethod>> {
                private final JavaClass targetOwner;
                private final TargetInfo target;

                MethodSupplier(JavaClass targetOwner, TargetInfo target) {
                    this.targetOwner = targetOwner;
                    this.target = target;
                }

                @Override
                public Optional<JavaMethod> get() {
                    return searchTargetMethod(targetOwner, target);
                }
            }
        }

        private static class FieldAccessTargetFactory implements AccessTargetFactory<FieldAccessTarget> {
            @Override
            public FieldAccessTarget create(JavaClass targetOwner, TargetInfo target, ImportedClasses classes) {
                Supplier<Optional<JavaField>> fieldSupplier = new FieldSupplier(targetOwner, target);
                JavaClass fieldType = classes.getOrResolve(JavaClassDescriptorImporter.importAsmTypeFromDescriptor(target.desc).getFullyQualifiedClassName());
                return new FieldAccessTargetBuilder()
                        .withOwner(targetOwner)
                        .withName(target.name)
                        .withType(fieldType)
                        .withMember(fieldSupplier)
                        .build();
            }

            private static class FieldSupplier implements Supplier<Optional<JavaField>> {
                private final JavaClass targetOwner;
                private final TargetInfo target;

                FieldSupplier(JavaClass targetOwner, TargetInfo target) {
                    this.targetOwner = targetOwner;
                    this.target = target;
                }

                @Override
                public Optional<JavaField> get() {
                    return searchTargetField(targetOwner, target);
                }
            }
        }

        private static class RawAccessRecordProcessed<TARGET extends AccessTarget> implements AccessRecord<TARGET> {
            private final RawAccessRecord record;
            private final ImportedClasses classes;
            private final JavaClass targetOwner;
            private final AccessTargetFactory<TARGET> accessTargetFactory;
            private final Supplier<JavaCodeUnit> originSupplier;

            RawAccessRecordProcessed(RawAccessRecord record, ImportedClasses classes, AccessTargetFactory<TARGET> accessTargetFactory) {
                this.record = record;
                this.classes = classes;
                targetOwner = this.classes.getOrResolve(record.target.owner.getFullyQualifiedClassName());
                this.accessTargetFactory = accessTargetFactory;
                originSupplier = createOriginSupplier(record.caller, classes);
            }

            @Override
            public JavaCodeUnit getOrigin() {
                return originSupplier.get();
            }

            @Override
            public TARGET getTarget() {
                return accessTargetFactory.create(targetOwner, record.target, classes);
            }

            @Override
            public int getLineNumber() {
                return record.lineNumber;
            }
        }

        private static class RawFieldAccessRecordProcessed extends RawAccessRecordProcessed<FieldAccessTarget> implements FieldAccessRecord {
            private final AccessType accessType;

            RawFieldAccessRecordProcessed(RawAccessRecord.ForField record, ImportedClasses classes) {
                super(record, classes, FIELD_ACCESS_TARGET_FACTORY);
                accessType = record.accessType;
            }

            @Override
            public AccessType getAccessType() {
                return accessType;
            }
        }

        private static Supplier<JavaCodeUnit> createOriginSupplier(final CodeUnit origin, final ImportedClasses classes) {
            return Suppliers.memoize(new Supplier<JavaCodeUnit>() {
                @Override
                public JavaCodeUnit get() {
                    return Factory.getOrigin(origin, classes);
                }
            });
        }

        private static JavaCodeUnit getOrigin(CodeUnit rawOrigin, ImportedClasses classes) {
            for (JavaCodeUnit method : classes.getOrResolve(rawOrigin.getDeclaringClassName()).getCodeUnits()) {
                if (rawOrigin.is(method)) {
                    return method;
                }
            }
            throw new IllegalStateException("Never found a " + JavaCodeUnit.class.getSimpleName() +
                    " that matches supposed origin " + rawOrigin);
        }

        private static List<JavaClass> getArgumentTypesFrom(String descriptor, ImportedClasses classes) {
            ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
            for (JavaClassDescriptor type : JavaClassDescriptorImporter.importAsmMethodArgumentTypes(descriptor)) {
                result.add(classes.getOrResolve(type.getFullyQualifiedClassName()));
            }
            return result.build();
        }

        private static Optional<JavaField> searchTargetField(JavaClass targetOwner, TargetInfo targetInfo) {
            Optional<JavaField> directlyFound = targetOwner.tryGetField(targetInfo.name);
            if (directlyFound.isPresent()) {
                return directlyFound;
            }

            // if a matching field has been found in an interface, it must be the one and only matching field,
            // since it is public static final and the compiler would forbid the call without disambiguation otherwise
            Optional<JavaField> foundOnInterface = searchFieldInInterfaces(targetOwner, targetInfo);
            if (foundOnInterface.isPresent()) {
                return foundOnInterface;
            }

            return searchFieldInSuperClass(targetOwner, targetInfo);
        }

        private static Optional<JavaField> searchFieldInInterfaces(JavaClass targetOwner, TargetInfo targetInfo) {
            for (JavaClass rawInterface : targetOwner.getRawInterfaces()) {
                Optional<JavaField> foundOnInterface = searchTargetField(rawInterface, targetInfo);
                if (foundOnInterface.isPresent()) {
                    return foundOnInterface;
                }
            }
            return Optional.empty();
        }

        private static Optional<JavaField> searchFieldInSuperClass(JavaClass targetOwner, TargetInfo targetInfo) {
            return targetOwner.getRawSuperclass().isPresent()
                    ? searchTargetField(targetOwner.getRawSuperclass().get(), targetInfo)
                    : Optional.<JavaField>empty();
        }

        private static Optional<JavaMethod> searchTargetMethod(JavaClass targetOwner, TargetInfo targetInfo) {
            MatchingMethods matchingMethods = new MatchingMethods(targetInfo);
            matchingMethods.addMatching(targetOwner.getMethods(), true);
            return matchingMethods.hasMatch()
                    // shortcut -> if we found it directly in the class we don't need to look further up the hierarchy
                    ? matchingMethods.determineMostSpecificMethod()
                    : searchTargetMethodInHierarchy(targetOwner, matchingMethods);
        }

        private static Optional<JavaMethod> searchTargetMethodInHierarchy(JavaClass targetOwner, MatchingMethods matchingMethods) {
            Optional<JavaClass> superclass = targetOwner.getRawSuperclass();
            if (superclass.isPresent()) {
                matchingMethods.addMatching(superclass.get().getMethods(), true);
                searchTargetMethodInHierarchy(superclass.get(), matchingMethods);
            }
            for (JavaClass interfaceType : targetOwner.getRawInterfaces()) {
                matchingMethods.addMatching(interfaceType.getMethods(), false);
                searchTargetMethodInHierarchy(interfaceType, matchingMethods);
            }
            return matchingMethods.determineMostSpecificMethod();
        }

        private static class MatchingMethods {
            private final TargetInfo target;
            private final LinkedHashMultimap<JavaClass, JavaMethod> matchingMethodsByReturnType = LinkedHashMultimap.create();

            private MatchingMethods(TargetInfo target) {
                this.target = target;
            }

            void addMatching(Collection<JavaMethod> methods, boolean includeStatic) {
                for (JavaMethod method : methods) {
                    if (matches(method, includeStatic)) {
                        matchingMethodsByReturnType.put(method.getRawReturnType(), method);
                    }
                }
            }

            private boolean matches(JavaMethod method, boolean includeStatic) {
                return method.getName().equals(target.name)
                        && method.getDescriptor().equals(target.desc)
                        && (includeStatic || !method.getModifiers().contains(STATIC));
            }

            boolean hasMatch() {
                return !matchingMethodsByReturnType.isEmpty();
            }

            /**
             * We roughly follow the algorithm of {@link Class#getMethod(String, Class[])}. We look for the most specific return type,
             * if there should be return types without a hierarchical correlation we simply pick the first. If there should be methods
             * with the same return type, but declaring classes without hierarchical correlation we will try to follow the JDK version,
             * even though it does not seem to be specified clearly (thus it could change with a different JDK implementation, but
             * unit tests should tell us).
             */
            Optional<JavaMethod> determineMostSpecificMethod() {
                if (!hasMatch()) {
                    return Optional.empty();
                }
                if (matchingMethodsByReturnType.size() == 1) {
                    return determineMostSpecificMethodWithSameReturnType(matchingMethodsByReturnType.values());
                }

                Collection<JavaMethod> methodsWithMostSpecificReturnType = determineMethodsWithMostSpecificReturnType(matchingMethodsByReturnType);
                return determineMostSpecificMethodWithSameReturnType(methodsWithMostSpecificReturnType);
            }

            private static Optional<JavaMethod> determineMostSpecificMethodWithSameReturnType(Collection<JavaMethod> methods) {
                JavaMethod result = null;
                for (JavaMethod method : methods) {
                    if (result == null || method.getOwner().isAssignableTo(result.getOwner().getName())) {
                        result = method;
                    }
                }
                return Optional.ofNullable(result);
            }

            private static Collection<JavaMethod> determineMethodsWithMostSpecificReturnType(LinkedHashMultimap<JavaClass, JavaMethod> matchingMethodsByReturnType) {
                Map.Entry<JavaClass, Collection<JavaMethod>> result = null;
                for (Map.Entry<JavaClass, Collection<JavaMethod>> entry : matchingMethodsByReturnType.asMap().entrySet()) {
                    if (result == null || entry.getKey().isAssignableTo(result.getKey().getName())) {
                        result = entry;
                    }
                }
                return result != null ? result.getValue() : Collections.<JavaMethod>emptySet();
            }
        }
    }
}
