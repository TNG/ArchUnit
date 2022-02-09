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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.ForwardingList;
import com.tngtech.archunit.base.MayResolveTypesViaReflection;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;
import com.tngtech.archunit.core.domain.properties.HasReturnType;
import com.tngtech.archunit.core.domain.properties.HasThrowsClause;
import com.tngtech.archunit.core.domain.properties.HasTypeParameters;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaCodeUnitBuilder;

import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.formatMethod;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;

/**
 * Represents a unit of code containing accesses to other units of code. A unit of code can be
 * <ul>
 * <li>a method</li>
 * <li>a constructor</li>
 * <li>a static initializer</li>
 * </ul>
 * in particular every place, where Java code with behavior, like calling other methods or accessing fields, can
 * be defined.
 */
public abstract class JavaCodeUnit
        extends JavaMember
        implements HasParameterTypes, HasReturnType, HasTypeParameters<JavaCodeUnit>, HasThrowsClause<JavaCodeUnit> {

    private final JavaType returnType;
    private final Parameters parameters;
    private final String fullName;
    private final List<JavaTypeVariable<JavaCodeUnit>> typeParameters;
    private final Set<ReferencedClassObject> referencedClassObjects;
    private final Set<InstanceofCheck> instanceofChecks;

    private Set<JavaFieldAccess> fieldAccesses = Collections.emptySet();
    private Set<JavaMethodCall> methodCalls = Collections.emptySet();
    private Set<JavaConstructorCall> constructorCalls = Collections.emptySet();
    private Set<JavaMethodReference> methodReferences = Collections.emptySet();
    private Set<JavaConstructorReference> constructorReferences = Collections.emptySet();

    JavaCodeUnit(JavaCodeUnitBuilder<?, ?> builder) {
        super(builder);
        typeParameters = builder.getTypeParameters(this);
        returnType = builder.getReturnType(this);
        parameters = new Parameters(this, builder);
        fullName = formatMethod(getOwner().getName(), getName(), namesOf(getRawParameterTypes()));
        referencedClassObjects = ImmutableSet.copyOf(builder.getReferencedClassObjects(this));
        instanceofChecks = ImmutableSet.copyOf(builder.getInstanceofChecks(this));
    }

    /**
     * @return The full name of this {@link JavaCodeUnit}, i.e. a string containing {@code ${declaringClass}.${name}(${parameterTypes})}
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getFullName() {
        return fullName;
    }

    /**
     * @return the raw parameter types of this {@link JavaCodeUnit}. On the contrary to {@link #getParameterTypes()}
     *         these will always be {@link JavaClass} and thus not containing any parameterization/generic
     *         information. Note that the raw parameter types can contain synthetic parameters added by the compiler.
     *         E.g. for inner class constructors that receive the outer class as synthetic parameter or enum constructors
     *         that receive enum name and ordinal as synthetic parameters. There is no guarantee about the number
     *         of synthetic parameters, nor if they are appended or prepended, as e.g. local classes will append
     *         all local variables from the outer scope that are referenced as additional synthetic constructor
     *         parameters.
     *
     * @see #getParameterTypes()
     * @see #getParameters()
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public List<JavaClass> getRawParameterTypes() {
        return parameters.getRawParameterTypes();
    }

    /**
     * @return the (possibly generic) parameter types of this {@link JavaCodeUnit}. This could for example be a
     *         {@link JavaTypeVariable} or a {@link JavaParameterizedType}, but also simply a {@link JavaClass}.<br>
     *         Note that <b>if</b> the method has a generic signature (e.g. declaring a parameterized type) then
     *         on the contrary to {@link #getRawParameterTypes()} these types will be parsed from the
     *         signature encoded in the bytecode, i.e. they will not contain synthetic parameters added by the
     *         compiler. However, if there is no generic signature, then this information can also not be parsed
     *         from the signature. In this case the parameter types will be equal to the raw parameter types
     *         and by that can also contain synthetic parameters.
     *
     * @see #getRawParameterTypes()
     * @see #getParameters()
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public List<JavaType> getParameterTypes() {
        return parameters.getParameterTypes();
    }

    /**
     * @return the {@link JavaParameter parameters} of this {@link JavaCodeUnit}. On the contrary to the Reflection API this will only contain
     *         the parameters from the signature and not synthetic parameters, if the signature is generic. In these cases
     *         {@link #getParameters()}{@code .size()} will always be equal to {@link #getParameterTypes()}{@code .size()},
     *         but not necessarily to {@link #getRawParameterTypes()}{@code .size()} in case the compiler adds synthetic parameters.<br>
     *         Note that for non-generic method signatures {@link #getParameters()} actually contains the raw parameter types and thus
     *         can also contain synthetic parameters. Unfortunately there is no way at the moment to distinguish synthetic
     *         parameters from non-synthetic parameters in these cases.
     *
     * @see #getRawParameterTypes()
     * @see #getParameterTypes()
     */
    @PublicAPI(usage = ACCESS)
    public List<JavaParameter> getParameters() {
        return parameters;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public abstract ThrowsClause<? extends JavaCodeUnit> getThrowsClause();

    /**
     * @return The types thrown by this method, similar to {@link Method#getExceptionTypes()}
     */
    @PublicAPI(usage = ACCESS)
    public List<JavaClass> getExceptionTypes() {
        return getThrowsClause().getTypes();
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaType getReturnType() {
        return returnType;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass getRawReturnType() {
        return returnType.toErasure();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaFieldAccess> getFieldAccesses() {
        return fieldAccesses;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodCall> getMethodCallsFromSelf() {
        return methodCalls;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorCall> getConstructorCallsFromSelf() {
        return constructorCalls;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaMethodReference> getMethodReferencesFromSelf() {
        return methodReferences;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaConstructorReference> getConstructorReferencesFromSelf() {
        return constructorReferences;
    }

    @PublicAPI(usage = ACCESS)
    public Set<ReferencedClassObject> getReferencedClassObjects() {
        return referencedClassObjects;
    }

    @PublicAPI(usage = ACCESS)
    public Set<InstanceofCheck> getInstanceofChecks() {
        return instanceofChecks;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaCall<?>> getCallsFromSelf() {
        return union(getMethodCallsFromSelf(), getConstructorCallsFromSelf());
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaCodeUnitReference<?>> getCodeUnitReferencesFromSelf() {
        return union(getMethodReferencesFromSelf(), getConstructorReferencesFromSelf());
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaAccess<?>> getAccessesFromSelf() {
        return ImmutableSet.<JavaAccess<?>>builder()
                .addAll(getCallsFromSelf())
                .addAll(getFieldAccesses())
                .addAll(getCodeUnitReferencesFromSelf())
                .build();
    }

    @PublicAPI(usage = ACCESS)
    public boolean isConstructor() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this code unit
    public Set<? extends JavaAnnotation<? extends JavaCodeUnit>> getAnnotations() {
        return (Set<? extends JavaAnnotation<? extends JavaCodeUnit>>) super.getAnnotations();
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this code unit
    public JavaAnnotation<? extends JavaCodeUnit> getAnnotationOfType(String typeName) {
        return (JavaAnnotation<? extends JavaCodeUnit>) super.getAnnotationOfType(typeName);
    }

    @Override
    @SuppressWarnings("unchecked") // we know the 'owning' member is this code unit
    public Optional<? extends JavaAnnotation<? extends JavaCodeUnit>> tryGetAnnotationOfType(String typeName) {
        return (Optional<? extends JavaAnnotation<? extends JavaCodeUnit>>) super.tryGetAnnotationOfType(typeName);
    }

    @PublicAPI(usage = ACCESS)
    public List<? extends JavaTypeVariable<? extends JavaCodeUnit>> getTypeParameters() {
        return typeParameters;
    }

    @PublicAPI(usage = ACCESS)
    public List<Set<JavaAnnotation<JavaParameter>>> getParameterAnnotations() {
        return parameters.getAnnotations();
    }

    void completeAccessesFrom(ImportContext context) {
        fieldAccesses = context.createFieldAccessesFor(this);
        methodCalls = context.createMethodCallsFor(this);
        constructorCalls = context.createConstructorCallsFor(this);
        methodReferences = context.createMethodReferencesFor(this);
        constructorReferences = context.createConstructorReferencesFor(this);
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution process")
    static Class<?>[] reflect(List<JavaClass> parameters) {
        List<Class<?>> result = new ArrayList<>();
        for (JavaClass parameter : parameters) {
            result.add(parameter.reflect());
        }
        return result.toArray(new Class<?>[0]);
    }

    private static class Parameters extends ForwardingList<JavaParameter> {
        private final List<JavaClass> rawParameterTypes;
        private final List<JavaType> parameterTypes;
        private final List<Set<JavaAnnotation<JavaParameter>>> parameterAnnotations;
        private final List<JavaParameter> parameters;

        Parameters(JavaCodeUnit owner, JavaCodeUnitBuilder<?, ?> builder) {
            rawParameterTypes = builder.getRawParameterTypes();
            parameterTypes = getParameterTypes(builder.getGenericParameterTypes(owner));
            parameters = createParameters(owner, builder, parameterTypes);
            parameterAnnotations = annotationsOf(parameters);
        }

        private List<Set<JavaAnnotation<JavaParameter>>> annotationsOf(List<JavaParameter> parameters) {
            ImmutableList.Builder<Set<JavaAnnotation<JavaParameter>>> result = ImmutableList.builder();
            for (JavaParameter parameter : parameters) {
                result.add(parameter.getAnnotations());
            }
            return result.build();
        }

        private static List<JavaParameter> createParameters(JavaCodeUnit owner, JavaCodeUnitBuilder<?, ?> builder, List<JavaType> parameterTypes) {
            ImmutableList.Builder<JavaParameter> result = ImmutableList.builder();
            for (int i = 0; i < parameterTypes.size(); i++) {
                result.add(new JavaParameter(owner, builder.getParameterAnnotationsBuilder(i), i, parameterTypes.get(i)));
            }
            return result.build();
        }

        @SuppressWarnings({"unchecked", "rawtypes"}) // the cast is safe because the list is immutable, thus used in a covariant way
        private List<JavaType> getParameterTypes(List<JavaType> genericParameterTypes) {
            return genericParameterTypes.isEmpty() ? (List) rawParameterTypes : genericParameterTypes;
        }

        List<JavaClass> getRawParameterTypes() {
            return rawParameterTypes;
        }

        List<JavaType> getParameterTypes() {
            return parameterTypes;
        }

        List<Set<JavaAnnotation<JavaParameter>>> getAnnotations() {
            return parameterAnnotations;
        }

        @Override
        protected List<JavaParameter> delegate() {
            return parameters;
        }
    }

    public static final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<JavaCodeUnit> constructor() {
            return new DescribedPredicate<JavaCodeUnit>("constructor") {
                @Override
                public boolean apply(JavaCodeUnit input) {
                    return input.isConstructor();
                }
            };
        }
    }

    @PublicAPI(usage = ACCESS)
    public static final class Functions {
        private Functions() {
        }

        public static final class Get {
            private Get() {
            }

            @PublicAPI(usage = ACCESS)
            public static <T extends JavaCodeUnit> ChainableFunction<T, ThrowsClause<T>> throwsClause() {
                return new ChainableFunction<T, ThrowsClause<T>>() {
                    // getThrowsClause() will always return a ThrowsClause typed to the owner, i.e. T
                    // We want to avoid that annoying recursive SELF type parameter and instead override covariantly...
                    @SuppressWarnings("unchecked")
                    @Override
                    public ThrowsClause<T> apply(T input) {
                        return (ThrowsClause<T>) input.getThrowsClause();
                    }
                };
            }
        }
    }
}
