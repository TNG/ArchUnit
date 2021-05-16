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

import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Represents a generic array type used in signatures of parameterized types.<br>
 * E.g. for {@code MyClass<A, T extends List<A[]>>} the upper bound {@code List<A[]>}
 * would have one {@link JavaGenericArrayType} {@code A[]} as its type parameter.<br>
 * Like its concrete counterpart a {@link JavaGenericArrayType} can be queried for its
 * {@link #getComponentType() component type}, which can be a {@link JavaParameterizedType},
 * a {@link JavaTypeVariable} or a {@link JavaGenericArrayType} corresponding to a lower dimensional array.
 */
@PublicAPI(usage = ACCESS)
public final class JavaGenericArrayType implements JavaType {
    private final String name;
    private final JavaType componentType;
    private final JavaClass erasure;

    JavaGenericArrayType(String name, JavaType componentType, JavaClass erasure) {
        this.name = checkNotNull(name);
        this.componentType = checkNotNull(componentType);
        this.erasure = checkNotNull(erasure);
    }

    /**
     * @return The name of this {@link JavaGenericArrayType}, e.g. for {@code A[]} within
     *         signature {@code MyClass<A, T extends List<A[]>>} the name would be "A[]"
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public String getName() {
        return name;
    }

    /**
     * @return The component type of this {@link JavaGenericArrayType}, e.g. for {@code A[]} within
     *         signature {@code MyClass<A, T extends List<A[]>>} the component type would be {@code A},
     *         while for {@code A[][]} within {@code MyClass<A, T extends List<A[][]>>} the component
     *         type would be {@code A[]}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaType getComponentType() {
        return componentType;
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
}
