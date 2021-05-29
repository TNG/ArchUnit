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

import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.HasUpperBounds;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaWildcardTypeBuilder;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Formatters.ensureCanonicalArrayTypeName;

/**
 * Represents a wildcard type in a type signature (compare the JLS).
 * Consider the generic type {@code List<T>}, then the parameterized type
 * {@code List<?>} would have the wildcard {@code ?} as its type argument
 * (also see {@link JavaParameterizedType}).<br>
 * According to the JLS a wildcard may have upper and lower bounds.<br>
 * An upper bound denotes a common supertype any substitution of this wildcard must
 * be assignable to. It is denoted by {@code ? extends SomeType}.<br>
 * A lower bound denotes a common subtype that must be assignable to all substitutions
 * of this wildcard type. It is denoted by {@code ? super SomeType}.
 */
public class JavaWildcardType implements JavaType, HasUpperBounds {
    private static final String WILDCARD_TYPE_NAME = "?";

    private final List<JavaType> upperBounds;
    private final List<JavaType> lowerBounds;
    private final JavaClass erasure;

    JavaWildcardType(JavaWildcardTypeBuilder<?> builder) {
        upperBounds = builder.getUpperBounds();
        lowerBounds = builder.getLowerBounds();
        erasure = builder.getUnboundErasureType(upperBounds);
    }

    /**
     * @return The name of this {@link JavaWildcardType}, which is always "{@code ?}",
     *         followed by the respective bounds if any are present
     *         (e.g. "{@code ? extends java.lang.String}")
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getName() {
        return WILDCARD_TYPE_NAME + boundsToString();
    }

    /**
     * @return All upper bounds of this {@link JavaWildcardType}, i.e. supertypes any substitution
     *         of this variable must extend. E.g. for
     *         {@code List<? extends SomeClass>} the upper bounds would be {@code [SomeClass]}<br>
     *         Note that the JLS currently only allows a single upper bound for a wildcard type,
     *         but we follow the Reflection API here and support a collection
     *         (compare {@link WildcardType#getUpperBounds()}).
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public List<JavaType> getUpperBounds() {
        return upperBounds;
    }

    /**
     * @return All lower bounds of this {@link JavaWildcardType}, i.e. any substitution for this
     *         {@link JavaWildcardType} must be a supertype of all lower bounds. E.g. for
     *         {@code Handler<? super SomeClass>>} the lower bounds would be {@code [SomeClass]}.<br>
     *         Note that the JLS currently only allows a single lower bound for a wildcard type,
     *         but we follow the Reflection API here and support a collection
     *         (compare {@link WildcardType#getLowerBounds()}).
     */
    @PublicAPI(usage = ACCESS)
    public List<JavaType> getLowerBounds() {
        return lowerBounds;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass toErasure() {
        return erasure;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + getName() + '}';
    }

    private String boundsToString() {
        String upperBoundsString = !upperBounds.isEmpty() ? " extends " + joinTypeNames(upperBounds) : "";
        String lowerBoundsString = !lowerBounds.isEmpty() ? " super " + joinTypeNames(lowerBounds) : "";
        return upperBoundsString + lowerBoundsString;
    }

    private String joinTypeNames(List<JavaType> types) {
        List<String> formatted = new ArrayList<>();
        for (JavaType type : types) {
            formatted.add(ensureCanonicalArrayTypeName(type.getName()));
        }
        return Joiner.on(" & ").join(formatted);
    }
}
