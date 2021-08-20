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
package com.tngtech.archunit.core.domain;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;
import com.tngtech.archunit.core.domain.properties.HasReturnType;
import com.tngtech.archunit.core.domain.properties.HasThrowsClause;
import com.tngtech.archunit.core.domain.properties.HasTypeParameters;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaCodeUnitBuilder;

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
    private final List<JavaClass> rawParameterTypes;
    private final List<JavaType> parameterTypes;
    private final String fullName;
    private final List<JavaTypeVariable<JavaCodeUnit>> typeParameters;
    private final Set<ReferencedClassObject> referencedClassObjects;
    private final Set<InstanceofCheck> instanceofChecks;

    private Set<JavaFieldAccess> fieldAccesses = Collections.emptySet();
    private Set<JavaMethodCall> methodCalls = Collections.emptySet();
    private Set<JavaConstructorCall> constructorCalls = Collections.emptySet();

    JavaCodeUnit(JavaCodeUnitBuilder<?, ?> builder) {
        super(builder);
        typeParameters = builder.getTypeParameters(this);
        returnType = builder.getReturnType(this);
        rawParameterTypes = builder.getRawParameterTypes();
        parameterTypes = getParameterTypes(builder);
        fullName = formatMethod(getOwner().getName(), getName(), namesOf(getRawParameterTypes()));
        referencedClassObjects = ImmutableSet.copyOf(builder.getReferencedClassObjects(this));
        instanceofChecks = ImmutableSet.copyOf(builder.getInstanceofChecks(this));
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // the cast is safe because the list is immutable, thus used in a covariant way
    private List<JavaType> getParameterTypes(JavaCodeUnitBuilder<?, ?> builder) {
        List<JavaType> genericParameterTypes = builder.getGenericParameterTypes(this);
        return genericParameterTypes.isEmpty() ? (List) rawParameterTypes : genericParameterTypes;
    }

    /**
     * @return The full name of this {@link JavaCodeUnit}, i.e. a string containing {@code ${declaringClass}.${name}(${parameterTypes})}
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getFullName() {
        return fullName;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public List<JavaClass> getRawParameterTypes() {
        return rawParameterTypes;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public List<JavaType> getParameterTypes() {
        return parameterTypes;
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
    public Set<ReferencedClassObject> getReferencedClassObjects() {
        return referencedClassObjects;
    }

    @PublicAPI(usage = ACCESS)
    public Set<InstanceofCheck> getInstanceofChecks() {
        return instanceofChecks;
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaCall<?>> getCallsFromSelf() {
        return ImmutableSet.<JavaCall<?>>builder()
                .addAll(getMethodCallsFromSelf())
                .addAll(getConstructorCallsFromSelf())
                .build();
    }

    @PublicAPI(usage = ACCESS)
    public Set<JavaAccess<?>> getAccessesFromSelf() {
        return ImmutableSet.<JavaAccess<?>>builder()
                .addAll(getCallsFromSelf())
                .addAll(getFieldAccesses())
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

    void completeAccessesFrom(ImportContext context) {
        fieldAccesses = context.createFieldAccessesFor(this);
        methodCalls = context.createMethodCallsFor(this);
        constructorCalls = context.createConstructorCallsFor(this);
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
