/*
 * Copyright 2019 TNG Technology Consulting GmbH
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.properties.HasDescriptor;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.regex.Pattern.quote;

class RawAccessRecord {
    final CodeUnit caller;
    final TargetInfo target;
    final int lineNumber;

    RawAccessRecord(CodeUnit caller, TargetInfo target, int lineNumber) {
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
        final RawAccessRecord other = (RawAccessRecord) obj;
        return Objects.equals(this.caller, other.caller) &&
                Objects.equals(this.target, other.target) &&
                Objects.equals(this.lineNumber, other.lineNumber);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + fieldsAsString() + '}';
    }

    private String fieldsAsString() {
        return "caller=" + caller + ", target=" + target + ", lineNumber=" + lineNumber;
    }

    static class CodeUnit {
        private final String name;
        private final List<String> parameters;
        private final String declaringClassName;
        private final int hashCode;

        CodeUnit(String name, List<String> parameters, String declaringClassName) {
            this.name = name;
            this.parameters = parameters;
            this.declaringClassName = declaringClassName;
            this.hashCode = Objects.hash(name, parameters, declaringClassName);
        }

        public String getName() {
            return name;
        }

        public List<String> getParameters() {
            return parameters;
        }

        String getDeclaringClassName() {
            return declaringClassName;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CodeUnit codeUnit = (CodeUnit) o;
            return Objects.equals(name, codeUnit.name) &&
                    Objects.equals(parameters, codeUnit.parameters) &&
                    Objects.equals(declaringClassName, codeUnit.declaringClassName);
        }

        @Override
        public String toString() {
            return "CodeUnit{" +
                    "name='" + name + '\'' +
                    ", parameters=" + parameters +
                    ", declaringClassName='" + declaringClassName + '\'' +
                    '}';
        }

        public boolean is(JavaCodeUnit method) {
            return getName().equals(method.getName())
                    && getParameters().equals(method.getRawParameterTypes().getNames())
                    && getDeclaringClassName().equals(method.getOwner().getName());
        }
    }

    abstract static class TargetInfo {
        final JavaType owner;
        final String name;
        final String desc;

        TargetInfo(String owner, String name, String desc) {
            this.owner = JavaTypeImporter.createFromAsmObjectTypeName(owner);
            this.name = name;
            this.desc = desc;
        }

        <T extends HasName & HasDescriptor & HasOwner<JavaClass>> boolean matches(T member) {
            if (!name.equals(member.getName()) || !desc.equals(member.getDescriptor())) {
                return false;
            }
            return owner.getName().equals(member.getOwner().getName()) ||
                    classHierarchyFrom(member).hasExactlyOneMatchFor(this);
        }

        private <T extends HasName & HasDescriptor & HasOwner<JavaClass>> ClassHierarchyPath classHierarchyFrom(T member) {
            return new ClassHierarchyPath(owner, member.getOwner());
        }

        protected abstract boolean signatureExistsIn(JavaClass javaClass);

        boolean hasMatchingSignatureTo(JavaMethod method) {
            return method.getName().equals(name) && method.getDescriptor().equals(desc);
        }

        boolean hasMatchingSignatureTo(JavaConstructor constructor) {
            return CONSTRUCTOR_NAME.equals(name) && constructor.getDescriptor().equals(desc);
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

        private static class ClassHierarchyPath {
            private final List<JavaClass> path = new ArrayList<>();

            private ClassHierarchyPath(JavaType childType, JavaClass parent) {
                Set<JavaClass> classesToSearchForChild = Sets.union(singleton(parent), parent.getAllSubClasses());
                Optional<JavaClass> child = tryFind(classesToSearchForChild, nameMatching(quote(childType.getName())));
                if (child.isPresent()) {
                    createPath(child.get(), parent);
                }
            }

            private static <T> Optional<T> tryFind(Iterable<T> collection, DescribedPredicate<? super T> predicate) {
                for (T elem : collection) {
                    if (predicate.apply(elem)) {
                        return Optional.of(elem);
                    }
                }
                return Optional.absent();
            }

            private void createPath(JavaClass child, JavaClass parent) {
                HierarchyResolutionStrategy hierarchyResolutionStrategy = hierarchyResolutionStrategyFrom(child).to(parent);
                path.add(child);
                while (hierarchyResolutionStrategy.hasNext()) {
                    path.add(hierarchyResolutionStrategy.next());
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

            private HierarchyResolutionStrategyCreator hierarchyResolutionStrategyFrom(JavaClass child) {
                return new HierarchyResolutionStrategyCreator(child);
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
                    return !current.equals(parent) && current.getSuperClass().isPresent();
                }

                @Override
                public JavaClass next() {
                    current = current.getSuperClass().get();
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
                    for (JavaClass i : child.getInterfaces()) {
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
                if (hasMatchingSignatureTo(constructor)) {
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
                if (hasMatchingSignatureTo(method)) {
                    return true;
                }
            }
            return false;
        }
    }

    static class Builder extends BaseBuilder<Builder> {
    }

    static class BaseBuilder<SELF extends BaseBuilder<SELF>> {
        CodeUnit caller;
        TargetInfo target;
        int lineNumber = -1;

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

        RawAccessRecord build() {
            return new RawAccessRecord(caller, target, lineNumber);
        }
    }

    static class ForField extends RawAccessRecord {
        final AccessType accessType;

        private ForField(CodeUnit caller, TargetInfo target, int lineNumber, AccessType accessType) {
            super(caller, target, lineNumber);
            this.accessType = accessType;
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + Objects.hash(accessType);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final ForField other = (ForField) obj;
            return Objects.equals(this.accessType, other.accessType);
        }

        static class Builder extends BaseBuilder<Builder> {
            private AccessType accessType;

            Builder withAccessType(AccessType accessType) {
                this.accessType = accessType;
                return this;
            }

            @Override
            ForField build() {
                return new ForField(super.caller, super.target, super.lineNumber, accessType);
            }
        }
    }
}
