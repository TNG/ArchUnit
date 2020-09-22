/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassDescriptor;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.DomainBuilders.ConstructorCallTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.FieldAccessTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.MethodCallTargetBuilder;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.importer.RawAccessRecord.TargetInfo;

import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;

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
                targetOwner = this.classes.getOrResolve(record.target.owner.getFullyQualifiedClassName());
                callerSupplier = createCallerSupplier(record.caller, classes);
            }

            @Override
            public JavaCodeUnit getCaller() {
                return callerSupplier.get();
            }

            @Override
            public ConstructorCallTarget getTarget() {
                Supplier<Optional<JavaConstructor>> constructorSupplier = new ConstructorTargetSupplier(targetOwner, record.target);
                List<JavaClass> paramTypes = getArgumentTypesFrom(record.target.desc, classes);
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

            private static class ConstructorTargetSupplier implements Supplier<Optional<JavaConstructor>> {
                private final JavaClass targetOwner;
                private final TargetInfo target;

                ConstructorTargetSupplier(JavaClass targetOwner, TargetInfo target) {
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

        private static class RawMethodCallRecordProcessed implements AccessRecord<MethodCallTarget> {
            private final RawAccessRecord record;
            final ImportedClasses classes;
            private final JavaClass targetOwner;
            private final Supplier<JavaCodeUnit> callerSupplier;

            RawMethodCallRecordProcessed(RawAccessRecord record, ImportedClasses classes) {
                this.record = record;
                this.classes = classes;
                targetOwner = this.classes.getOrResolve(record.target.owner.getFullyQualifiedClassName());
                callerSupplier = createCallerSupplier(record.caller, classes);
            }

            @Override
            public JavaCodeUnit getCaller() {
                return callerSupplier.get();
            }

            @Override
            public MethodCallTarget getTarget() {
                Supplier<Optional<JavaMethod>> methodsSupplier = new MethodTargetSupplier(targetOwner, record.target);
                List<JavaClass> parameters = getArgumentTypesFrom(record.target.desc, classes);
                JavaClass returnType = classes.getOrResolve(JavaClassDescriptorImporter.importAsmMethodReturnType(record.target.desc).getFullyQualifiedClassName());
                return new MethodCallTargetBuilder()
                        .withOwner(targetOwner)
                        .withName(record.target.name)
                        .withParameters(parameters)
                        .withReturnType(returnType)
                        .withMethod(methodsSupplier)
                        .build();
            }

            @Override
            public int getLineNumber() {
                return record.lineNumber;
            }

            private static class MethodTargetSupplier implements Supplier<Optional<JavaMethod>> {
                private final JavaClass targetOwner;
                private final TargetInfo target;

                MethodTargetSupplier(JavaClass targetOwner, TargetInfo target) {
                    this.targetOwner = targetOwner;
                    this.target = target;
                }

                @Override
                public Optional<JavaMethod> get() {
                    return searchTargetMethod(targetOwner, target);
                }
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
                targetOwner = this.classes.getOrResolve(record.target.owner.getFullyQualifiedClassName());
                callerSupplier = createCallerSupplier(record.caller, classes);
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
                Supplier<Optional<JavaField>> fieldSupplier = new FieldTargetSupplier(targetOwner, record.target);
                JavaClass fieldType = classes.getOrResolve(JavaClassDescriptorImporter.importAsmTypeFromDescriptor(record.target.desc).getFullyQualifiedClassName());
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

            private static class FieldTargetSupplier implements Supplier<Optional<JavaField>> {
                private final JavaClass targetOwner;
                private final TargetInfo target;

                FieldTargetSupplier(JavaClass targetOwner, TargetInfo target) {
                    this.targetOwner = targetOwner;
                    this.target = target;
                }

                @Override
                public Optional<JavaField> get() {
                    return searchTargetField(targetOwner, target);
                }
            }
        }

        private static Supplier<JavaCodeUnit> createCallerSupplier(final CodeUnit caller, final ImportedClasses classes) {
            return Suppliers.memoize(new Supplier<JavaCodeUnit>() {
                @Override
                public JavaCodeUnit get() {
                    return Factory.getCaller(caller, classes);
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
