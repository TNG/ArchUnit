package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;
import org.objectweb.asm.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.JavaClass.withType;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.JavaStaticInitializer.STATIC_INITIALIZER_NAME;
import static java.util.Collections.singleton;

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

    String fieldsAsString() {
        return "caller=" + caller + ", target=" + target + ", lineNumber=" + lineNumber;
    }

    static class CodeUnit {
        private final String name;
        private final List<TypeDetails> parameters;
        private final String declaringClassName;
        private final int hashCode;

        private CodeUnit(Member member) {
            this(nameOf(member),
                    parametersOf(member),
                    member.getDeclaringClass(),
                    Objects.hash(member));
        }

        private CodeUnit(String name, List<TypeDetails> parameters, Class<?> declaringClass, int hashCode) {
            this(name, parameters, declaringClass.getName(), hashCode);
        }

        private CodeUnit(String name, List<TypeDetails> parameters, String declaringClassName, int hashCode) {
            this.name = name;
            this.parameters = parameters;
            this.declaringClassName = declaringClassName;
            this.hashCode = hashCode;
        }

        private static List<TypeDetails> parametersOf(Member member) {
            return member instanceof Constructor ?
                    TypeDetails.allOf(((Constructor<?>) member).getParameterTypes()) :
                    TypeDetails.allOf(((Method) member).getParameterTypes());
        }

        private static String nameOf(Member member) {
            return member instanceof Constructor ?
                    CONSTRUCTOR_NAME :
                    member.getName();
        }

        public String getName() {
            return name;
        }

        @SuppressWarnings("unchecked")
        public List<TypeDetails> getParameters() {
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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
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

        static CodeUnit of(Object o) {
            checkArgument(o instanceof Constructor || o instanceof Method);
            return new CodeUnit((Member) o);
        }

        static CodeUnit staticInitializerOf(final Class<?> clazz) {
            return new StaticInitializer(clazz.getName());
        }

        public boolean is(JavaCodeUnit<?, ?> method) {
            return getName().equals(method.getName())
                    && getParameters().equals(method.getParameters())
                    && getDeclaringClassName().equals(method.getOwner().getName());
        }

        private static class StaticInitializer extends CodeUnit {
            private StaticInitializer(String className) {
                super(STATIC_INITIALIZER_NAME, Collections.<TypeDetails>emptyList(), className, Objects.hash(STATIC_INITIALIZER_NAME, className));
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
                final StaticInitializer other = (StaticInitializer) obj;
                return Objects.equals(getName(), other.getName()) &&
                        Objects.equals(getDeclaringClassName(), other.getDeclaringClassName()) &&
                        Objects.equals(getParameters(), other.getParameters());
            }

            @Override
            public String toString() {
                return String.format("%s{owner=%s, name=%s}", getClass().getSimpleName(), getDeclaringClassName(), getName());
            }
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

        boolean hasMatchingSignatureTo(Field field) {
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

            boolean hasExactlyOneMatchFor(final TargetInfo target) {
                Set<JavaClass> matching = new HashSet<>();
                for (JavaClass javaClass : path) {
                    if (target.signatureExistsIn(javaClass)) {
                        matching.add(javaClass);
                    }
                }
                return matching.size() == 1;
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

        RawAccessRecord buildAccessRecord() {
            return new RawAccessRecord(caller, target, lineNumber);
        }
    }
}
