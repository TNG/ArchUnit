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
package com.tngtech.archunit.core.domain;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;
import com.tngtech.archunit.core.domain.properties.HasReturnType;
import com.tngtech.archunit.core.domain.properties.HasThrowsClause;
import com.tngtech.archunit.core.domain.properties.HasType;
import com.tngtech.archunit.core.importer.DomainBuilders.AccessTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.CodeUnitAccessTargetBuilder;
import com.tngtech.archunit.core.importer.DomainBuilders.FieldAccessTargetBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.JavaCodeUnit.Functions.Get.throwsClause;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;

/**
 * Represents the target of a {@link JavaAccess}. ArchUnit distinguishes between an 'access target' and a concrete field/method/constructor,
 * because the bytecode does not reflect the exact member that an access will resolve to. Take for example
 * <pre><code>
 * class Caller {
 *     void call(Target target) {
 *         target.call();
 *     }
 * }
 *
 * class Target extends TargetParent {
 * }
 *
 * class TargetParent {
 *     void call() {}
 * }
 * </code></pre>
 * Then the bytecode will have encoded {@code Caller -> Target.call()}, but there is no method {@code call()} in {@code Target} since
 * it needs to be resolved from the parent. This can be a lot more complex for interface inheritance, where multiple inheritance is
 * possible.<br><br>
 * To determine the respective member targeted by a {@link JavaAccess} ArchUnit follows the logic of the Reflection API
 * (e.g. {@link Class#getMethod(String, Class[])}). This only applies to <b>how</b> members are located, not what members are located
 * (e.g. ArchUnit <b>will</b> find a {@link JavaCodeUnit} with name {@value com.tngtech.archunit.core.domain.JavaConstructor#CONSTRUCTOR_NAME},
 * even if the Reflection API explicitly excludes this bytecode-only method and represents the constructor as a method with the simple class
 * name instead).
 * <br><br>
 * Note that it is possible that ArchUnit will not find any member matching this {@link AccessTarget}. This is due to the fact that any
 * numbers of referenced classes can be missing from the import. E.g. some method {@code Foo.origin()} of some imported
 * class {@code Foo} might call a method {@code Bar.target()}. But if {@code Bar}
 * is missing from the import (i.e. the bytecode of {@code Bar.class} has not been scanned together with {@code Foo.class}),
 * there will not be a {@link JavaMethod} representing {@code Bar.target()}.
 * So even though we can derive an {@link AccessTarget} that is {@code Bar.target()}
 * from {@code Foo's} bytecode (including method name and parameters), we cannot associate any {@link JavaMethod} with that target.
 *
 * @see #resolveMember()
 */
public abstract class AccessTarget implements HasName.AndFullName, CanBeAnnotated, HasOwner<JavaClass>, HasDescription {
    private final String name;
    private final JavaClass owner;
    private final String fullName;
    private final Supplier<? extends Optional<? extends JavaMember>> member;

    AccessTarget(AccessTargetBuilder<?, ?, ?> builder) {
        this.name = checkNotNull(builder.getName());
        this.owner = checkNotNull(builder.getOwner());
        this.fullName = checkNotNull(builder.getFullName());
        this.member = Suppliers.memoize(builder.getMember());
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

    /**
     * @deprecated This will never return more than one element, use {@link #resolveMember()} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public abstract Set<? extends JavaMember> resolve();

    /**
     * Attempts to resolve the targeted member (method, field or constructor).
     * This will be a single element if the target has been imported,
     * or {@link Optional#empty()} if the target was not imported.
     * For further information refer to {@link AccessTarget}.
     *
     * @see MethodCallTarget#resolveMember()
     * @see FieldAccessTarget#resolveMember()
     * @see ConstructorCallTarget#resolveMember()
     *
     * @return The member that matches the access target or empty if it was not imported
     */
    @PublicAPI(usage = ACCESS)
    public Optional<? extends JavaMember> resolveMember() {
        return member.get();
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
     * @see #isAnnotatedWith(Class)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isAnnotatedWith(final String annotationTypeName) {
        return resolveMember().isPresent() && resolveMember().get().isAnnotatedWith(annotationTypeName);
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
        return resolveMember().isPresent() && resolveMember().get().isAnnotatedWith(predicate);
    }

    /**
     * Returns true, if one of the resolved targets is meta-annotated with an annotation of the given type.<br>
     * NOTE: If the target was not imported, this method will always return false.
     *
     * @param annotationType Type of the annotation to look for
     * @return true if one of the resolved targets is meta-annotated with an annotation with the given type
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(Class<? extends Annotation> annotationType) {
        return isMetaAnnotatedWith(annotationType.getName());
    }

    /**
     * @see #isMetaAnnotatedWith(Class)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(final String annotationTypeName) {
        return resolveMember().isPresent() && resolveMember().get().isMetaAnnotatedWith(annotationTypeName);
    }

    /**
     * Returns true, if one of the resolved targets is meta-annotated with an annotation matching the predicate.<br>
     * NOTE: If the target was not imported, this method will always return false.
     *
     * @param predicate Qualifies matching annotations
     * @return true if one of the resolved targets is meta-annotated with an annotation matching the predicate
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public boolean isMetaAnnotatedWith(final DescribedPredicate<? super JavaAnnotation<?>> predicate) {
        return resolveMember().isPresent() && resolveMember().get().isMetaAnnotatedWith(predicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName);
    }

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

    public static final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<AccessTarget, Optional<JavaMember>> RESOLVE_MEMBER =
                new ChainableFunction<AccessTarget, Optional<JavaMember>>() {
                    @SuppressWarnings("unchecked") // Optional is covariant
                    @Override
                    public Optional<JavaMember> apply(AccessTarget input) {
                        return (Optional<JavaMember>) input.resolveMember();
                    }
                };

        /**
         * @deprecated Use {@link #RESOLVE_MEMBER} instead
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<AccessTarget, Set<JavaMember>> RESOLVE = RESOLVE_MEMBER.then(new Function<Optional<JavaMember>, Set<JavaMember>>() {
            @Override
            public Set<JavaMember> apply(Optional<JavaMember> input) {
                return input.asSet();
            }
        });
    }

    /**
     * Represents an {@link AccessTarget} where the target is a field. For further elaboration about the necessity to distinguish
     * {@link FieldAccessTarget FieldAccessTarget} from {@link JavaField}, refer to the documentation at {@link AccessTarget}.
     */
    public static final class FieldAccessTarget extends AccessTarget implements HasType {
        private final JavaClass type;

        FieldAccessTarget(FieldAccessTargetBuilder builder) {
            super(builder);
            this.type = checkNotNull(builder.getType());
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public JavaType getType() {
            return type;
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public JavaClass getRawType() {
            return type;
        }

        /**
         * @deprecated Use {@link #resolveMember()} instead
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public Optional<JavaField> resolveField() {
            return resolveMember();
        }

        /**
         * @deprecated This will never return more than one element, use {@link #resolveMember()} instead
         */
        @Override
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public Set<JavaField> resolve() {
            return resolveMember().asSet();
        }

        /**
         * Attempts to resolve an imported field that has the same type and name as this target.<br>
         * The result will be the only accessible field that the origin can reach. I.e. if there are multiple
         * fields with the same type and name in the hierarchy of the target, then the one that is accessible
         * will be picked. This must be unique, otherwise the compiler would force disambiguating by an
         * explicit cast. Consider e.g.
         *
         * <pre><code>
         * interface A {
         *     Class&lt;?&gt; value = A.class;
         * }
         *
         * interface B {
         *     Class&lt;?&gt; value = B.class;
         * }
         *
         * class C implements A, B {
         * }
         *
         * class X {
         *     C c;
         *     // ...
         *     Class&lt;?&gt; origin() {
         *         return ((B) c).value;
         *     }
         * }
         * </code></pre>
         * Without the cast {@code ((B) c)} this code would not compile because the field reference is ambiguous.<br>
         * Note that the result can still be {@link Optional#empty()} in case the class containing the field has not
         * been imported.
         *
         * @return The field that matches this target, or {@link Optional#empty()} if no matching field was imported.
         */
        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // cast is safe because we know the member must be a JavaField if present
        public Optional<JavaField> resolveMember() {
            return (Optional<JavaField>) super.resolveMember();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public String getDescription() {
            return "field <" + getFullName() + ">";
        }

        public static final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<FieldAccessTarget, Optional<JavaField>> RESOLVE_MEMBER =
                    new ChainableFunction<FieldAccessTarget, Optional<JavaField>>() {
                        @Override
                        public Optional<JavaField> apply(FieldAccessTarget input) {
                            return input.resolveMember();
                        }
                    };

            /**
             * @deprecated Use {@link #RESOLVE_MEMBER} instead
             */
            @Deprecated
            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<FieldAccessTarget, Set<JavaField>> RESOLVE = RESOLVE_MEMBER.then(new Function<Optional<JavaField>, Set<JavaField>>() {
                @Override
                public Set<JavaField> apply(Optional<JavaField> input) {
                    return input.asSet();
                }
            });
        }
    }

    /**
     * Represents an {@link AccessTarget} where the target is a code unit. For further elaboration about the necessity to distinguish
     * {@link CodeUnitAccessTarget CodeUnitAccessTarget} from {@link JavaCodeUnit}, refer to the documentation at {@link AccessTarget}.
     */
    public abstract static class CodeUnitAccessTarget extends AccessTarget
            implements HasParameterTypes, HasReturnType, HasThrowsClause<CodeUnitAccessTarget> {
        private final ImmutableList<JavaClass> parameters;
        private final JavaClass returnType;

        CodeUnitAccessTarget(CodeUnitAccessTargetBuilder<?, ?> builder) {
            super(builder);
            this.parameters = ImmutableList.copyOf(builder.getParameters());
            this.returnType = checkNotNull(builder.getReturnType());
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"}) // cast is okay, since this list can only be used in a covariant way (immutable)
        public List<JavaType> getParameterTypes() {
            return (List) parameters;
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public List<JavaClass> getRawParameterTypes() {
            return parameters;
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public JavaType getReturnType() {
            return returnType;
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public JavaClass getRawReturnType() {
            return returnType;
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public ThrowsClause<? extends CodeUnitAccessTarget> getThrowsClause() {
            Optional<ThrowsClause<JavaCodeUnit>> resolvedThrowsClauses = resolveMember().map(throwsClause());

            if (resolvedThrowsClauses.isPresent()) {
                return ThrowsClause.from(this, resolvedThrowsClauses.get().getTypes());
            } else {
                return ThrowsClause.empty(this);
            }
        }

        /**
         * Tries to resolve the targeted method or constructor.
         *
         * @see ConstructorCallTarget#resolveMember()
         * @see MethodCallTarget#resolveMember()
         */
        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // cast is safe because we know the member must be a JavaCodeUnit if present
        public Optional<? extends JavaCodeUnit> resolveMember() {
            return (Optional<? extends JavaCodeUnit>) super.resolveMember();
        }

        /**
         * @deprecated This will never return more than one element, use {@link #resolveMember()} instead
         */
        @Override
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public abstract Set<? extends JavaCodeUnit> resolve();

        public static final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<CodeUnitAccessTarget, Optional<JavaCodeUnit>> RESOLVE_MEMBER =
                    new ChainableFunction<CodeUnitAccessTarget, Optional<JavaCodeUnit>>() {
                        @SuppressWarnings("unchecked") // Optional is covariant
                        @Override
                        public Optional<JavaCodeUnit> apply(CodeUnitAccessTarget input) {
                            return (Optional<JavaCodeUnit>) input.resolveMember();
                        }
                    };

            /**
             * @deprecated Use {@link #RESOLVE_MEMBER} instead
             */
            @Deprecated
            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<CodeUnitAccessTarget, Set<JavaCodeUnit>> RESOLVE = RESOLVE_MEMBER.then(new Function<Optional<JavaCodeUnit>, Set<JavaCodeUnit>>() {
                @Override
                public Set<JavaCodeUnit> apply(Optional<JavaCodeUnit> input) {
                    return input.asSet();
                }
            });
        }
    }

    /**
     * Represents an {@link AccessTarget} where the target is a code unit. For further elaboration about the necessity to distinguish
     * {@link CodeUnitCallTarget CodeUnitCallTarget} from {@link JavaCodeUnit} refer to the documentation at {@link AccessTarget}.
     */
    public abstract static class CodeUnitCallTarget extends CodeUnitAccessTarget {
        CodeUnitCallTarget(CodeUnitAccessTargetBuilder<?, ?> builder) {
            super(builder);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // the cast is safe because the type parameter represents this object
        public ThrowsClause<? extends CodeUnitCallTarget> getThrowsClause() {
            return (ThrowsClause<CodeUnitCallTarget>) super.getThrowsClause();
        }
    }

    /**
     * Represents an {@link AccessTarget} where the target is a code unit. For further elaboration about the necessity to distinguish
     * {@link CodeUnitReferenceTarget CodeUnitReferenceTarget} from {@link JavaCodeUnit} refer to the documentation at {@link AccessTarget}.
     */
    public abstract static class CodeUnitReferenceTarget extends CodeUnitAccessTarget {
        CodeUnitReferenceTarget(CodeUnitAccessTargetBuilder<?, ?> builder) {
            super(builder);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // the cast is safe because the type parameter represents this object
        public ThrowsClause<? extends CodeUnitReferenceTarget> getThrowsClause() {
            return (ThrowsClause<CodeUnitReferenceTarget>) super.getThrowsClause();
        }
    }

    /**
     * Represents a {@link CodeUnitCallTarget} where the target is a constructor. For further elaboration about the necessity to distinguish
     * {@link ConstructorCallTarget ConstructorCallTarget} from {@link JavaConstructor} refer to the documentation at {@link AccessTarget}.
     */
    public static final class ConstructorCallTarget extends CodeUnitCallTarget {
        ConstructorCallTarget(CodeUnitAccessTargetBuilder<JavaConstructor, ConstructorCallTarget> builder) {
            super(builder);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // the cast is safe because the type parameter represents this object
        public ThrowsClause<ConstructorCallTarget> getThrowsClause() {
            return (ThrowsClause<ConstructorCallTarget>) super.getThrowsClause();
        }

        /**
         * @deprecated Use {@link #resolveMember()} instead
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public Optional<JavaConstructor> resolveConstructor() {
            return resolveMember();
        }

        /**
         * @deprecated This will never return more than one element, use {@link #resolveMember()} instead
         */
        @Override
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public Set<JavaConstructor> resolve() {
            return resolveMember().asSet();
        }

        /**
         * @return A constructor that matches this target, or {@link Optional#empty()} if no matching constructor
         * was imported.
         */
        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // cast is safe because we know the member must be a JavaConstructor if present
        public Optional<JavaConstructor> resolveMember() {
            return (Optional<JavaConstructor>) super.resolveMember();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public String getDescription() {
            return "constructor <" + getFullName() + ">";
        }

        public static final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<ConstructorCallTarget, Optional<JavaConstructor>> RESOLVE_MEMBER =
                    new ChainableFunction<ConstructorCallTarget, Optional<JavaConstructor>>() {
                        @Override
                        public Optional<JavaConstructor> apply(ConstructorCallTarget input) {
                            return input.resolveMember();
                        }
                    };

            /**
             * @deprecated Use {@link #RESOLVE_MEMBER} instead
             */
            @Deprecated
            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<ConstructorCallTarget, Set<JavaConstructor>> RESOLVE = RESOLVE_MEMBER.then(new Function<Optional<JavaConstructor>, Set<JavaConstructor>>() {
                @Override
                public Set<JavaConstructor> apply(Optional<JavaConstructor> input) {
                    return input.asSet();
                }
            });
        }
    }

    /**
     * Represents a {@link CodeUnitReferenceTarget} where the target is a constructor. For further elaboration about the necessity to distinguish
     * {@link ConstructorReferenceTarget ConstructorReferenceTarget} from {@link JavaConstructor} refer to the documentation at {@link AccessTarget}.
     */
    public static final class ConstructorReferenceTarget extends CodeUnitReferenceTarget {
        ConstructorReferenceTarget(CodeUnitAccessTargetBuilder<JavaConstructor, ConstructorReferenceTarget> builder) {
            super(builder);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // the cast is safe because the type parameter represents this object
        public ThrowsClause<ConstructorReferenceTarget> getThrowsClause() {
            return (ThrowsClause<ConstructorReferenceTarget>) super.getThrowsClause();
        }

        /**
         * @deprecated This will never return more than one element, use {@link #resolveMember()} instead
         */
        @Override
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public Set<JavaConstructor> resolve() {
            return resolveMember().asSet();
        }

        /**
         * @return A constructor that matches this target, or {@link Optional#empty()} if no matching constructor
         * was imported.
         */
        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // cast is safe because we know the member must be a JavaConstructor if present
        public Optional<JavaConstructor> resolveMember() {
            return (Optional<JavaConstructor>) super.resolveMember();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public String getDescription() {
            return "constructor <" + getFullName() + ">";
        }

        public static final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<ConstructorReferenceTarget, Set<JavaConstructor>> RESOLVE_MEMBER =
                    new ChainableFunction<ConstructorReferenceTarget, Set<JavaConstructor>>() {
                        @Override
                        public Set<JavaConstructor> apply(ConstructorReferenceTarget input) {
                            return input.resolve();
                        }
                    };
        }
    }

    /**
     * Represents a {@link CodeUnitCallTarget} where the target is a method. For further elaboration about the necessity to distinguish
     * {@link MethodCallTarget MethodCallTarget} from {@link JavaMethod} refer to the documentation at {@link AccessTarget}.
     */
    public static final class MethodCallTarget extends CodeUnitCallTarget {
        MethodCallTarget(CodeUnitAccessTargetBuilder<JavaMethod, MethodCallTarget> builder) {
            super(builder);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // the cast is safe because the type parameter represents this object
        public ThrowsClause<MethodCallTarget> getThrowsClause() {
            return (ThrowsClause<MethodCallTarget>) super.getThrowsClause();
        }

        /**
         * Attempts to resolve an imported method that matches this target. Note that while usually the respective method is
         * clear from the context, there are more complicated scenarios. Consider for example
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
         * At runtime the target will be clear, but considering only this bytecode there seem to be two valid candidates
         * we could pick as the referenced method. To resolve this ambiguity ArchUnit follows the logic described
         * in the Java Reflection API, i.e. {@link Class#getMethod(String, Class[])}.
         *
         * @return Matching method if imported, {@link Optional#empty()} otherwise
         */
        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // cast is safe because we know the member must be a JavaMethod if present
        public Optional<JavaMethod> resolveMember() {
            return (Optional<JavaMethod>) super.resolveMember();
        }

        /**
         * @deprecated This will never return more than one element, use {@link #resolveMember()} instead
         */
        @Override
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public Set<JavaMethod> resolve() {
            return resolveMember().asSet();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public String getDescription() {
            return "method <" + getFullName() + ">";
        }

        public static final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<MethodCallTarget, Optional<JavaMethod>> RESOLVE_MEMBER =
                    new ChainableFunction<MethodCallTarget, Optional<JavaMethod>>() {
                        @Override
                        public Optional<JavaMethod> apply(MethodCallTarget input) {
                            return input.resolveMember();
                        }
                    };

            /**
             * @deprecated Use {@link #RESOLVE_MEMBER} instead
             */
            @Deprecated
            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<MethodCallTarget, Set<JavaMethod>> RESOLVE = RESOLVE_MEMBER.then(new Function<Optional<JavaMethod>, Set<JavaMethod>>() {
                @Override
                public Set<JavaMethod> apply(Optional<JavaMethod> input) {
                    return input.asSet();
                }
            });
        }
    }

    /**
     * Represents a {@link CodeUnitReferenceTarget} where the target is a method. For further elaboration about the necessity to distinguish
     * {@link MethodReferenceTarget MethodReferenceTarget} from {@link JavaMethod} refer to the documentation at {@link AccessTarget}.
     */
    public static final class MethodReferenceTarget extends CodeUnitReferenceTarget {
        MethodReferenceTarget(CodeUnitAccessTargetBuilder<JavaMethod, MethodReferenceTarget> builder) {
            super(builder);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // the cast is safe because the type parameter represents this object
        public ThrowsClause<MethodReferenceTarget> getThrowsClause() {
            return (ThrowsClause<MethodReferenceTarget>) super.getThrowsClause();
        }

        /**
         * Attempts to resolve an imported method that matches this target.
         * The process is the same as described at {@link MethodCallTarget#resolveMember() MethodCallTarget.resolveMember()}.
         *
         * @return Matching method if imported, {@link Optional#empty()} otherwise
         */
        @Override
        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked") // cast is safe because we know the member must be a JavaMethod if present
        public Optional<JavaMethod> resolveMember() {
            return (Optional<JavaMethod>) super.resolveMember();
        }

        /**
         * @deprecated This will never return more than one element, use {@link #resolveMember()} instead
         */
        @Override
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public Set<JavaMethod> resolve() {
            return resolveMember().asSet();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public String getDescription() {
            return "method <" + getFullName() + ">";
        }

        public static final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<MethodReferenceTarget, Optional<JavaMethod>> RESOLVE_MEMBER =
                    new ChainableFunction<MethodReferenceTarget, Optional<JavaMethod>>() {
                        @Override
                        public Optional<JavaMethod> apply(MethodReferenceTarget input) {
                            return input.resolveMember();
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
                    .forSubtype();
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
