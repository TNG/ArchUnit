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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.DomainBuilders.ConstructorCallTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.FieldAccessTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.MethodCallTargetBuilder;
import com.tngtech.archunit.core.importer.RawAccessRecord.CodeUnit;
import com.tngtech.archunit.core.importer.RawAccessRecord.TargetInfo;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.singletonList;

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
                    return Optional.absent();
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
                Supplier<Set<JavaMethod>> methodsSupplier = new MethodTargetSupplier(targetOwner.getAllMethods(), record.target);
                List<JavaClass> parameters = getArgumentTypesFrom(record.target.desc, classes);
                JavaClass returnType = classes.getOrResolve(JavaClassDescriptorImporter.importAsmMethodReturnType(record.target.desc).getFullyQualifiedClassName());
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

            private static class MethodTargetSupplier implements Supplier<Set<JavaMethod>> {
                private final Set<JavaMethod> allMethods;
                private final TargetInfo target;

                MethodTargetSupplier(Set<JavaMethod> allMethods, TargetInfo target) {
                    this.allMethods = allMethods;
                    this.target = target;
                }

                @Override
                public Set<JavaMethod> get() {
                    return tryFindMatchingTargets(allMethods, target, METHOD_SIGNATURE_PREDICATE);
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
                Supplier<Optional<JavaField>> fieldSupplier = new FieldTargetSupplier(targetOwner.getAllFields(), record.target);
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
                private final Set<JavaField> allFields;
                private final TargetInfo target;

                FieldTargetSupplier(Set<JavaField> allFields, TargetInfo target) {
                    this.allFields = allFields;
                    this.target = target;
                }

                @Override
                public Optional<JavaField> get() {
                    return uniqueTargetIn(tryFindMatchingTargets(allFields, target, FIELD_SIGNATURE_PREDICATE));
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

        private static <MEMBER extends JavaMember, TARGET extends TargetInfo> Set<MEMBER>
        tryFindMatchingTargets(Set<MEMBER> possibleTargets, TARGET target, SignaturePredicate<TARGET> signaturePredicate) {
            ImmutableSet.Builder<MEMBER> result = ImmutableSet.builder();
            for (MEMBER possibleTarget : possibleTargets) {
                if (matches(possibleTarget, target, signaturePredicate)) {
                    result.add(possibleTarget);
                }
            }
            return result.build();
        }

        private static <MEMBER extends JavaMember, TARGET extends TargetInfo> boolean matches(MEMBER member, TARGET target, SignaturePredicate<TARGET> signaturePredicate) {
            if (!target.name.equals(member.getName()) || !target.desc.equals(member.getDescriptor())) {
                return false;
            }
            return target.owner.getFullyQualifiedClassName().equals(member.getOwner().getName()) ||
                    containsExactlyOneMatch(new ClassHierarchyPath(target.owner, member.getOwner()), target, signaturePredicate);
        }

        private static <TARGET extends TargetInfo> boolean containsExactlyOneMatch(Iterable<JavaClass> classes, TARGET target, SignaturePredicate<TARGET> signaturePredicate) {
            Set<JavaClass> matching = new HashSet<>();
            for (JavaClass javaClass : classes) {
                if (signaturePredicate.exists(javaClass, target)) {
                    matching.add(javaClass);
                }
            }
            return matching.size() == 1;
        }

        private interface SignaturePredicate<TARGET extends TargetInfo> {
            boolean exists(JavaClass clazz, TARGET target);
        }

        private static final SignaturePredicate<TargetInfo> FIELD_SIGNATURE_PREDICATE = new SignaturePredicate<TargetInfo>() {
            @Override
            public boolean exists(JavaClass clazz, TargetInfo target) {
                Optional<JavaField> field = clazz.tryGetField(target.name);
                return field.isPresent() && target.desc.equals(field.get().getDescriptor());
            }
        };

        private static final SignaturePredicate<TargetInfo> METHOD_SIGNATURE_PREDICATE = new SignaturePredicate<TargetInfo>() {
            @Override
            public boolean exists(JavaClass clazz, TargetInfo target) {
                for (JavaMethod method : clazz.getMethods()) {
                    if (method.getName().equals(target.name) && method.getDescriptor().equals(target.desc)) {
                        return true;
                    }
                }
                return false;
            }
        };

        private static <T> Optional<T> uniqueTargetIn(Collection<T> collection) {
            return collection.size() == 1 ? Optional.of(getOnlyElement(collection)) : Optional.<T>absent();
        }

        private static List<JavaClass> getArgumentTypesFrom(String descriptor, ImportedClasses classes) {
            ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
            for (JavaClassDescriptor type : JavaClassDescriptorImporter.importAsmMethodArgumentTypes(descriptor)) {
                result.add(classes.getOrResolve(type.getFullyQualifiedClassName()));
            }
            return result.build();
        }

        private static class ClassHierarchyPath implements Iterable<JavaClass> {
            private final List<JavaClass> path;

            public ClassHierarchyPath(JavaClassDescriptor childType, JavaClass parent) {
                Optional<JavaClass> child = tryFindChildInHierarchy(childType, parent);
                path = child.isPresent() ? createPath(parent, child.get()) : Collections.<JavaClass>emptyList();
            }

            private Optional<JavaClass> tryFindChildInHierarchy(JavaClassDescriptor childType, JavaClass parent) {
                for (JavaClass subclass : parent.getAllSubclasses()) {
                    if (subclass.getName().equals(childType.getFullyQualifiedClassName())) {
                        return Optional.of(subclass);
                    }
                }
                return Optional.absent();
            }

            private List<JavaClass> createPath(JavaClass parent, JavaClass child) {
                ImmutableList.Builder<JavaClass> pathBuilder = ImmutableList.<JavaClass>builder().add(child);
                HierarchyResolutionStrategy hierarchyResolutionStrategy = hierarchyResolutionStrategyFrom(child).to(parent);
                while (hierarchyResolutionStrategy.hasNext()) {
                    pathBuilder.add(hierarchyResolutionStrategy.next());
                }
                return pathBuilder.build();
            }

            private HierarchyResolutionStrategyCreator hierarchyResolutionStrategyFrom(JavaClass child) {
                return new HierarchyResolutionStrategyCreator(child);
            }

            @Override
            public Iterator<JavaClass> iterator() {
                return path.iterator();
            }

            private interface HierarchyResolutionStrategy {
                boolean hasNext();

                JavaClass next();
            }

            private static class HierarchyResolutionStrategyCreator {
                private final JavaClass child;

                private HierarchyResolutionStrategyCreator(JavaClass child) {
                    this.child = child;
                }

                public HierarchyResolutionStrategy to(JavaClass parent) {
                    return parent.isInterface() ?
                            new InterfaceHierarchyResolutionStrategy(child, parent) :
                            new ClassHierarchyResolutionStrategy(child, parent);
                }
            }

            private static class ClassHierarchyResolutionStrategy implements HierarchyResolutionStrategy {
                private final JavaClass parent;
                private JavaClass current;

                private ClassHierarchyResolutionStrategy(JavaClass child, JavaClass parent) {
                    this.current = child;
                    this.parent = parent;
                }

                @Override
                public boolean hasNext() {
                    return !current.equals(parent) && current.getRawSuperclass().isPresent();
                }

                @Override
                public JavaClass next() {
                    current = current.getRawSuperclass().get();
                    return current;
                }
            }

            private static class InterfaceHierarchyResolutionStrategy implements HierarchyResolutionStrategy {
                private final Iterator<JavaClass> interfaces;
                private final JavaClass parent;
                private JavaClass current;

                private InterfaceHierarchyResolutionStrategy(JavaClass child, JavaClass parent) {
                    interfaces = interfacesBetween(child, parent);
                    this.parent = parent;
                    current = child;
                }

                private Iterator<JavaClass> interfacesBetween(JavaClass from, JavaClass target) {
                    Node node = new Node(from);
                    List<JavaClass> result = new ArrayList<>();
                    for (Node parent : node.parents) {
                        result.addAll(parent.to(target));
                    }
                    return result.iterator();
                }

                @Override
                public boolean hasNext() {
                    return !current.equals(parent) && interfaces.hasNext();
                }

                @Override
                public JavaClass next() {
                    current = interfaces.next();
                    return current;
                }
            }

            private static class Node {
                private final JavaClass child;
                private final Set<Node> parents = new HashSet<>();

                private Node(JavaClass child) {
                    this.child = child;
                    for (JavaClass i : child.getRawInterfaces()) {
                        parents.add(new Node(i));
                    }
                }

                public List<JavaClass> to(JavaClass target) {
                    if (child.equals(target)) {
                        return singletonList(child);
                    }
                    Set<JavaClass> result = new LinkedHashSet<>();
                    for (Node parent : parents) {
                        if (parent.contains(target)) {
                            result.add(child);
                            result.addAll(parent.to(target));
                        }
                    }
                    return new ArrayList<>(result);
                }

                public boolean contains(JavaClass target) {
                    if (child.equals(target)) {
                        return true;
                    }
                    for (Node parent : parents) {
                        if (parent.contains(target)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
    }
}
