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

import java.util.List;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * A {@link JavaParameterizedType} represents a concrete parameterization of a generic type.
 * Consider the generic type {@code List<T>}, then {@code List<String>} would be a parameterized type
 * where the concrete type {@code java.lang.String} has been assigned to the type variable {@code T}.
 * The concrete type {@code java.lang.String} is then an "actual type argument" of this {@link JavaParameterizedType}
 * (see {@link JavaParameterizedType#getActualTypeArguments()}).
 */
@PublicAPI(usage = ACCESS)
public interface JavaParameterizedType extends JavaType {
    /**
     * @return The actual type arguments of this parameterized type (compare {@link JavaParameterizedType}).
     */
    @PublicAPI(usage = ACCESS)
    List<JavaType> getActualTypeArguments();
}
