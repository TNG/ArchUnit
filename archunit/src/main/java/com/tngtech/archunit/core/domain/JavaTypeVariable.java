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

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaTypeVariableBuilder;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;

/**
 * Represents a type variable used by generic types and members.<br>
 * E.g. {@code class MyClass<T>} would have one {@link JavaTypeVariable} with name "T"
 * and unbound, i.e. only bound by {@link Object}.<br>
 * A type variable can have several bounds, where only one bound may be a class bound
 * while all further bounds must be interfaces (compare the JLS).<br>
 * Example: {@code class MyClass<T extends SomeClass & SomeInterfaceOne & SomeInterfaceTwo>}
 * would declare one {@link JavaTypeVariable} {@code T} which is bound by {@code SomeClass},
 * {@code SomeInterfaceOne} and {@code SomeInterfaceTwo}. I.e. any concrete class
 * substituted for the type variable must extend {@code SomeClass} and implement
 * {@code SomeInterfaceOne} and {@code SomeInterfaceTwo}.
 */
@PublicAPI(usage = ACCESS)
public final class JavaTypeVariable implements JavaType {
    private final String name;
    private final List<JavaType> bounds;
    private final JavaClass erasure;

    JavaTypeVariable(JavaTypeVariableBuilder builder) {
        name = builder.getName();
        bounds = builder.getBounds();
        erasure = bounds.size() > 0 ? bounds.get(0).toErasure() : builder.getUnboundErasureType();
    }

    /**
     * @return The name of this {@link JavaTypeVariable}, e.g. for {@code class MyClass<T>}
     *         the name would be "T"
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getName() {
        return name;
    }

    /**
     * @return All bounds of this {@link JavaTypeVariable}, i.e. super types any substitution
     *         of this variable must extend. E.g. for
     *         {@code class MyClass<T extends SomeClass & SomeInterface>} the bounds would be
     *         {@code SomeClass} and {@code SomeInterface}
     */
    @PublicAPI(usage = ACCESS)
    public List<JavaType> getBounds() {
        return bounds;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public JavaClass toErasure() {
        return erasure;
    }

    @Override
    public String toString() {
        String bounds = printExtendsClause() ? " extends " + joinTypeNames(this.bounds) : "";
        return getClass().getSimpleName() + '{' + getName() + bounds + '}';
    }

    private boolean printExtendsClause() {
        if (bounds.isEmpty()) {
            return false;
        }
        if (bounds.size() > 1) {
            return true;
        }
        return !getOnlyElement(bounds).getName().equals(Object.class.getName());
    }

    private String joinTypeNames(List<JavaType> types) {
        return FluentIterable.from(types).transform(toGuava(GET_NAME)).join(Joiner.on(" & "));
    }
}
