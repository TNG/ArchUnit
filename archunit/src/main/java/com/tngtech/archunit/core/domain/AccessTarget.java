/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;
import com.tngtech.archunit.core.domain.properties.HasReturnType;
import com.tngtech.archunit.core.domain.properties.HasThrowsClause;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.core.importer.DomainBuilders.CodeUnitCallTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.ConstructorCallTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.FieldAccessTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.MethodCallTargetBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;

/**
 * Represents the target of a {@link JavaAccess}. ArchUnit distinguishes between an 'access target' and a concrete field/method/constructor, because
 * the bytecode does not allow a 1-to-1 association here.
 * <br><br>
 * For one part, the target might be missing from the import, e.g. some method
 * <code>Foo.origin()</code> of some imported class <code>Foo</code> might call a method <code>Bar.target()</code>. But if <code>Bar</code>
 * is missing from the import (i.e. the bytecode of <code>Bar.class</code> has not been scanned together with <code>Foo.class</code>),
 * there will not be a {@link JavaMethod} representing <code>Bar.target()</code>.
 * So even though we can derive an {@link AccessTarget} that is <code>Bar.target()</code>
 * from <code>Foo's</code> bytecode (including method name and parameters), we cannot associate any {@link JavaMethod} with that target.
 * <br><br>
 * For the other part, even if all the participating classes are imported, there are still situations, where the respective access target cannot
 * be associated with one single {@link JavaMethod}. I.e. some diamond scenarios, where two interfaces <code>A</code> and <code>B</code>
 * both declare a method <code>target()</code>, interface <code>C</code> extends both, and some third party calls <code>C.target()</code>.
 * For further elaboration refer to the documentation of {@link #resolve()}. In particular {@link #resolve()} attempts to find
 * matching {@link JavaMember JavaMembers} for the respective {@link AccessTarget}.
 */
public abstract class AccessTarget implements HasName.AndFullName, CanBeAnnotated, HasOwner<JavaClass> {
    private final String name;
    private final JavaClass owner;
    private final String fullName;

    AccessTarget(JavaClass owner, String name, String fullName) {
        this.name = name;
        this.owner = owner;
        this.fullName = fullName;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getName() {
        return name;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass getOwner() {
        return owner;
    }

    /**
     * @return The full name of this {@link AccessTarget}, i.e. a string containing {@code ${declaringClass}.${name}} for a field and
     *         {@code ${declaringClass}.${name}(${parameterTypes})} for a code unit
     */
    @Override
    @PublicAPI(usage = ACCESS)
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
     * For further information refer to {@link AccessTarget}.
     *
     * @see MethodCallTarget#resolve()
     * @see FieldAccessTarget#resolve()
     * @see ConstructorCallTarget#resolve()
     *
     * @return Set of all members that match the call target
     */
    @PublicAPI(usage = ACCESS)
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
     * Returns true, if one of the resolved targets is annotated with the given annotation type.<br>
     * NOTE: If the target was not imported, this method will always return false.
     *
     * @param annotationType The type of the annotation to check for
     * @return true if one of the resolved targets is annotated with the given type
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(Class<? extends Annotation> annotationType) {
        return isAnnotatedWith(annotationType.getName());
    }

    /**
     * @see AccessTarget#isAnnotatedWith(Class)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(final String annotationTypeName) {
        return anyMember(new Predicate<JavaMember>() {
            @Override
            public boolean apply(JavaMember input) {
                return input.isAnnotatedWith(annotationTypeName);
            }
        });
    }

    /**
     * Returns true, if one of the resolved targets is annotated with an annotation matching the predicate.<br>
     * NOTE: If the target was not imported, this method will always return false.
     *
     * @param predicate Qualifies matching annotations
     * @return true if one of the resolved targets is annotated with an annotation matching the predicate
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(final DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return anyMember(new Predicate<JavaMember>() {
            @Override
            public boolean apply(JavaMember input) {
                return input.isAnnotatedWith(predicate);
            }
        });
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return isMetaAnnotatedWith(annotationType.getName());
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(final String annotationTypeName) {
        return anyMember(new Predicate<JavaMember>() {
            @Override
            public boolean apply(JavaMember input) {
                return input.isMetaAnnotatedWith(annotationTypeName);
            }
        });
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(final DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return anyMember(new Predicate<JavaMember>() {
            @Override
            public boolean apply(JavaMember input) {
                return input.isMetaAnnotatedWith(predicate);
            }
        });
    }

    private boolean anyMember(Predicate<JavaMember> predicate) {
        for (final JavaMember member : resolve()) {
            if (predicate.apply(member)) {
                return true;
            }
        }
        return false;
    }

    abstract String getDescription();

    public static final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<AccessTarget, Set<JavaMember>> RESOLVE =
                new ChainableFunction<AccessTarget, Set<JavaMember>>() {
                    @SuppressWarnings("unchecked") // Set is covariant
                    @Override
                    public Set<JavaMember> apply(AccessTarget input) {
                        return (Set<JavaMember>) input.resolve();
                    }
                };
    }

    /**
     * Represents an {@link AccessTarget} where the target is a field. For further elaboration about the necessity to distinguish
     * {@link FieldAccessTarget FieldAccessTarget} from {@link JavaField}, refer to the documentation at {@link AccessTarget}.
     */
    public static final class FieldAccessTarget extends AccessTarget implements HasType {
        private final JavaClass type;
        private final Supplier<Optional<JavaField>> field;

        FieldAccessTarget(FieldAccessTargetBuilder builder) {
            super(builder.getOwner(), builder.getName(), builder.getFullName());
            this.type = builder.getType();
            this.field = Suppliers.memoize(builder.getField());
        }

        /**
         * @deprecated Use {@link #getRawType()} instead
         */
        @Override
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public JavaClass getType() {
            return getRawType();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public JavaClass getRawType() {
            return type;
        }

        /**
         * @return A field that matches this target, or {@link Optional#absent()} if no matching field was imported.
         */
        @PublicAPI(usage = ACCESS)
        public Optional<JavaField> resolveField() {
            return field.get();
        }

        /**
         * @return Fields that match the target, this will always be either one field, or no field
         * @see #resolveField()
         */
        @Override
        @PublicAPI(usage = ACCESS)
        public Set<JavaField> resolve() {
            return resolveField().asSet();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        String getDescription() {
            return "field <" + getFullName() + ">";
        }

        public static final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<FieldAccessTarget, Set<JavaField>> RESOLVE =
                    new ChainableFunction<FieldAccessTarget, Set<JavaField>>() {
                        @Override
                        public Set<JavaField> apply(FieldAccessTarget input) {
                            return input.resolve();
                        }
                    };
        }
    }

    /**
     * Represents an {@link AccessTarget} where the target is a code unit. For further elaboration about the necessity to distinguish
     * {@link CodeUnitCallTarget CodeUnitCallTarget} from {@link JavaCodeUnit}, refer to the documentation at {@link AccessTarget} and in particular the
     * documentation at {@link MethodCallTarget#resolve() MethodCallTarget.resolve()}.
     */
    public abstract static class CodeUnitCallTarget extends AccessTarget
            implements HasParameterTypes, HasReturnType, HasThrowsClause<CodeUnitCallTarget> {
        private final ImmutableList<JavaClass> parameters;
        private final JavaClass returnType;

        CodeUnitCallTarget(CodeUnitCallTargetBuilder<?> builder) {
            super(builder.getOwner(), builder.getName(), builder.getFullName());
            this.parameters = ImmutableList.copyOf(builder.getParameters());
            this.returnType = builder.getReturnType();
        }

        /**
         * @deprecated Use {@link #getRawParameterTypes()} instead
         */
        @Override
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public JavaClassList getParameters() {
            return getRawParameterTypes();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public JavaClassList getRawParameterTypes() {
            return DomainObjectCreationContext.createJavaClassList(parameters);
        }

        /**
         * @deprecated Use {@link #getRawReturnType()} instead.
         */
        @Override
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public JavaClass getReturnType() {
            return getRawReturnType();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public JavaClass getRawReturnType() {
            return returnType;
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public ThrowsClause<CodeUnitCallTarget> getThrowsClause() {
            List<ThrowsClause<JavaCodeUnit>> resolvedThrowsClauses = FluentIterable.from(resolve())
                    .transform(toGuava(JavaCodeUnit.Functions.Get.throwsClause()))
                    .toList();

            if (resolvedThrowsClauses.isEmpty()) {
                return ThrowsClause.empty(this);
            } else if (resolvedThrowsClauses.size() == 1) {
                return ThrowsClause.from(this, getOnlyElement(resolvedThrowsClauses).getTypes());
            } else {
                return ThrowsClause.from(this, intersectTypesOf(resolvedThrowsClauses));
            }
        }

        private List<JavaClass> intersectTypesOf(List<ThrowsClause<JavaCodeUnit>> throwsClauses) {
            checkArgument(throwsClauses.size() > 1, "Can only intersect more than one throws clause");

            List<JavaClass> result = new ArrayList<>(throwsClauses.get(0).getTypes());
            for (ThrowsClause<?> throwsClause : throwsClauses.subList(1, throwsClauses.size())) {
                result.retainAll(throwsClause.getTypes());
            }
            return result;
        }

        /**
         * Tries to resolve the targeted method or constructor.
         *
         * @see ConstructorCallTarget#resolveConstructor()
         * @see MethodCallTarget#resolve()
         */
        @Override
        @PublicAPI(usage = ACCESS)
        public abstract Set<? extends JavaCodeUnit> resolve();

        public static final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<CodeUnitCallTarget, Set<JavaCodeUnit>> RESOLVE =
                    new ChainableFunction<CodeUnitCallTarget, Set<JavaCodeUnit>>() {
                        @SuppressWarnings("unchecked") // Set is covariant
                        @Override
                        public Set<JavaCodeUnit> apply(CodeUnitCallTarget input) {
                            return (Set<JavaCodeUnit>) input.resolve();
                        }
                    };
        }
    }

    public static final class ConstructorCallTarget extends CodeUnitCallTarget {
        private final Supplier<Optional<JavaConstructor>> constructor;

        ConstructorCallTarget(ConstructorCallTargetBuilder builder) {
            super(builder);
            constructor = builder.getConstructor();
        }

        /**
         * @return A constructor that matches this target, or {@link Optional#absent()} if no matching constructor
         * was imported.
         */
        @PublicAPI(usage = ACCESS)
        public Optional<JavaConstructor> resolveConstructor() {
            return constructor.get();
        }

        /**
         * @return constructors that match the target, this will always be either one constructor, or no constructor
         * @see #resolveConstructor()
         */
        @Override
        @PublicAPI(usage = ACCESS)
        public Set<JavaConstructor> resolve() {
            return resolveConstructor().asSet();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        String getDescription() {
            return "constructor <" + getFullName() + ">";
        }

        public static final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<ConstructorCallTarget, Set<JavaConstructor>> RESOLVE =
                    new ChainableFunction<ConstructorCallTarget, Set<JavaConstructor>>() {
                        @Override
                        public Set<JavaConstructor> apply(ConstructorCallTarget input) {
                            return input.resolve();
                        }
                    };
        }
    }

    /**
     * Represents a {@link CodeUnitCallTarget} where the target is a method. For further elaboration about the necessity to distinguish
     * {@link MethodCallTarget MethodCallTarget} from {@link JavaMethod}, refer to the documentation at {@link AccessTarget} and in particular the
     * documentation at {@link #resolve()}.
     */
    public static final class MethodCallTarget extends CodeUnitCallTarget {
        private final Supplier<Set<JavaMethod>> methods;

        MethodCallTarget(MethodCallTargetBuilder builder) {
            super(builder);
            this.methods = Suppliers.memoize(builder.getMethods());
        }

        /**
         * Attempts to resolve imported methods that match this target. Note that while usually there is one unique
         * target (if imported), it is possible that the call is ambiguous. For example consider
         * <pre><code>
         * interface A {
         *     void target();
         * }
         *
         * interface B {
         *     void target();
         * }
         *
         * interface C extends A, B {}
         *
         * class X {
         *     C c;
         *     // ...
         *     void origin() {
         *         c.target();
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
         * Note that the target would be uniquely determinable, if C would declare <code>void target()</code> itself.
         *
         * @return Set of matching methods, usually a single target
         */
        @Override
        @PublicAPI(usage = ACCESS)
        public Set<JavaMethod> resolve() {
            return methods.get();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        String getDescription() {
            return "method <" + getFullName() + ">";
        }

        public static final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<MethodCallTarget, Set<JavaMethod>> RESOLVE =
                    new ChainableFunction<MethodCallTarget, Set<JavaMethod>>() {
                        @Override
                        public Set<JavaMethod> apply(MethodCallTarget input) {
                            return input.resolve();
                        }
                    };
        }
    }

    public static final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<AccessTarget> declaredIn(Class<?> clazz) {
            return declaredIn(clazz.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<AccessTarget> declaredIn(String className) {
            return declaredIn(GET_NAME.is(equalTo(className)).as(className));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<AccessTarget> declaredIn(DescribedPredicate<? super JavaClass> predicate) {
            return Get.<JavaClass>owner().is(predicate)
                    .as("declared in %s", predicate.getDescription())
                    .forSubType();
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<AccessTarget> constructor() {
            return new DescribedPredicate<AccessTarget>("constructor") {
                @Override
                public boolean apply(AccessTarget input) {
                    return CONSTRUCTOR_NAME.equals(input.getName()); // The constructor name is sufficiently unique
                }
            };
        }
    }
}
