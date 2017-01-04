package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.properties.CanBeAnnotated;
import com.tngtech.archunit.core.properties.HasName;
import com.tngtech.archunit.core.properties.HasOwner;
import com.tngtech.archunit.core.properties.HasParameters;

import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;

public abstract class AccessTarget implements HasName.AndFullName, HasOwner<JavaClass> {
    private final String name;
    private final JavaClass owner;
    private final String fullName;

    AccessTarget(JavaClass owner, String name, String fullName) {
        this.name = name;
        this.owner = owner;
        this.fullName = fullName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JavaClass getOwner() {
        return owner;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName);
    }

    /**
     * Tries to resolve the targeted members (methods, fields or constructors). In most cases this will be a
     * single element, if the target was imported, or an empty set, if the target was not imported. However,
     * for {@link MethodCallTarget MethodCallTargets}, there can be multiple possible targets.
     *
     * @see MethodCallTarget#resolve()
     * @see FieldAccessTarget#resolve()
     * @see ConstructorCallTarget#resolve()
     */
    public abstract Set<? extends JavaMember> resolve();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final AccessTarget other = (AccessTarget) obj;
        return Objects.equals(this.fullName, other.fullName);
    }

    @Override
    public String toString() {
        return "target{" + fullName + '}';
    }

    /**
     * Returns true, if one of the resolved targets is annotated with the given annotation type.<br/>
     * NOTE: If the target was not imported, this method will always return false.
     *
     * @param annotation The type of the annotation to check for
     * @return true if one of the resolved targets is annotated with the given type
     */
    public boolean isAnnotatedWith(Class<? extends Annotation> annotation) {
        for (JavaMember member : resolve()) {
            if (member.isAnnotatedWith(annotation)) {
                return true;
            }
        }
        return false;
    }

    public static class FieldAccessTarget extends AccessTarget {
        private final JavaClass type;
        private final Supplier<Optional<JavaField>> field;

        FieldAccessTarget(JavaClass owner, String name, JavaClass type, Supplier<Optional<JavaField>> field) {
            super(owner, name, owner.getName() + "." + name);
            this.type = type;
            this.field = Suppliers.memoize(field);
        }

        public JavaClass getType() {
            return type;
        }

        /**
         * @return A field that matches this target, or {@link Optional#absent()} if no matching field was imported.
         */
        public Optional<JavaField> resolveField() {
            return field.get();
        }

        /**
         * @return Fields that match the target, this will always be either one field, or no field
         * @see #resolveField()
         */
        @Override
        public Set<JavaField> resolve() {
            return resolveField().asSet();
        }
    }

    public static abstract class CodeUnitCallTarget extends AccessTarget implements HasParameters, CanBeAnnotated {
        private final ImmutableList<JavaClass> parameters;
        private final JavaClass returnType;

        CodeUnitCallTarget(JavaClass owner, String name, JavaClassList parameters, JavaClass returnType) {
            super(owner, name, Formatters.formatMethod(owner.getName(), name, parameters));
            this.parameters = ImmutableList.copyOf(parameters);
            this.returnType = returnType;
        }

        @Override
        public JavaClassList getParameters() {
            return new JavaClassList(parameters);
        }

        public JavaClass getReturnType() {
            return returnType;
        }

        /**
         * Tries to resolve the targeted method or constructor.
         *
         * @see ConstructorCallTarget#resolveConstructor()
         * @see MethodCallTarget#resolve()
         */
        public abstract Set<? extends JavaCodeUnit> resolve();
    }

    public static class ConstructorCallTarget extends CodeUnitCallTarget {
        private final Supplier<Optional<JavaConstructor>> constructor;

        ConstructorCallTarget(JavaClass owner, JavaClassList parameters, JavaClass returnType, Supplier<Optional<JavaConstructor>> constructor) {
            super(owner, CONSTRUCTOR_NAME, parameters, returnType);
            this.constructor = Suppliers.memoize(constructor);
        }

        /**
         * @return A constructor that matches this target, or {@link Optional#absent()} if no matching constructor
         * was imported.
         */
        public Optional<JavaConstructor> resolveConstructor() {
            return constructor.get();
        }

        /**
         * @return constructors that match the target, this will always be either one constructor, or no constructor
         * @see #resolveConstructor()
         */
        @Override
        public Set<JavaConstructor> resolve() {
            return resolveConstructor().asSet();
        }
    }

    public static class MethodCallTarget extends CodeUnitCallTarget {
        private final Supplier<Set<JavaMethod>> methods;

        MethodCallTarget(JavaClass owner, String name, JavaClassList parameters,
                         JavaClass returnType, Supplier<Set<JavaMethod>> methods) {
            super(owner, name, parameters, returnType);
            this.methods = Suppliers.memoize(methods);
        }

        /**
         * Attempts to resolve imported methods that match this target. Note that while usually there is one unique
         * target (if imported), it is possible that the call is ambiguous. For example consider
         * <pre><code>
         * interface A {
         *     void foo();
         * }
         *
         * interface B {
         *     void foo();
         * }
         *
         * interface D extends A, B {}
         *
         * class X {
         *     D d;
         *     // ...
         *     void bar() {
         *         d.foo();
         *     }
         * }
         * </code></pre>
         * While, for any concrete implementation, the compiler will naturally resolve one concrete target to link to,
         * and thus at runtime the called target ist clear, from an analytical point of view the relevant target
         * can't be uniquely identified here. To sum up, the result can be
         * <ul>
         * <li>empty - if no imported method matches the target</li>
         * <li>a single method - if the method was imported and can uniquely be identified</li>
         * <li>several methods - in scenarios where there is no unique method that matches the target</li>
         * </ul>
         * Note that the target would be uniquely determinable, if D would declare <code>void foo()</code> itself.
         *
         * @return Set of matching methods, usually a single target
         */
        public Set<JavaMethod> resolve() {
            return methods.get();
        }
    }
}
