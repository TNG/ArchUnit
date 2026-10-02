/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * {@link JavaAnnotatedType} representing an array usage with potentially present annotations.</br>
 * Every array type contains an {@link #getAnnotatedComponentType() annotated component type} which can itself be a plain class, array, generic type or type parameter.</br>
 * Note that both the array type and the component type can be annotated. E.g. in {@code @A1 String @A2[]}, the array type is annotated with exactly {@code @A2} and the component type exactly with  {@code @A1}.</br>
 * This can be very deceptive in cases like {@code @NonNull String[]} which means an array of non-null Strings - but the array itself might be null.
 */
public interface JavaAnnotatedArrayType extends JavaAnnotatedType {

    /**
     * @return {@link JavaType} used by this {@code JavaAnnotatedType} without any type annotation information. It is either a {@link JavaGenericArrayType} or a {@link JavaClass} representing an Array class
     */
    @Override
    @PublicAPI(usage = ACCESS)
    JavaType getType();

    /**
     * @return {@code JavaAnnotatedType} representing the components of this array, e.g. {@code @A1 String} for {@code @A1 String @A2[]}.
     */
    @PublicAPI(usage = ACCESS)
    JavaAnnotatedType getAnnotatedComponentType();
}
