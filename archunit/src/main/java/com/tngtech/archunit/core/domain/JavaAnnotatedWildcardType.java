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

import java.lang.reflect.AnnotatedWildcardType;
import java.util.List;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * {@link JavaAnnotatedType} representing a usage of a {@link JavaWildcardType}.</br>
 * You can get the upper and lower bounds of the wildcard with {@link #getAnnotatedUpperBounds()} and {@link #getAnnotatedLowerBounds()}.
 * These methods use Lists for consistency with the java reflection API even though it is currently not possible in java to have multiple bounds at all.
 */
public interface JavaAnnotatedWildcardType extends JavaAnnotatedType {

    @Override
    @PublicAPI(usage = ACCESS)
    JavaWildcardType getType();

    /**
     * @return All upper bounds including annotations of this {@link JavaAnnotatedWildcardType}, i.e. supertypes any substitution
     *         of this variable must extend. E.g. for
     *         {@code List<? extends @TypeUseAnnoation SomeClass>} the upper bounds would be {@code [@TypeUseAnnoation  SomeClass]}<br>
     *         Note that the JLS currently only allows a single upper bound for a wildcard type,
     *         but we follow the Reflection API here and support a collection
     *         (compare {@link AnnotatedWildcardType#getAnnotatedUpperBounds()}).
     */
    @PublicAPI(usage = ACCESS)
    List<JavaAnnotatedType> getAnnotatedUpperBounds();

    /**
     * @return All lower bounds including annotations of this {@link JavaAnnotatedWildcardType}, i.e. any substitution for this
     *         {@link JavaWildcardType} must be a supertype of all lower bounds. E.g. for
     *         {@code Handler<? super @TypeUseAnnoation SomeClass>>} the lower bounds would be {@code [@TypeUseAnnoation SomeClass]}.<br>
     *         Note that the JLS currently only allows a single lower bound for a wildcard type,
     *         but we follow the Reflection API here and support a collection
     *         (compare {@link AnnotatedWildcardType#getAnnotatedLowerBounds()}).
     */
    @PublicAPI(usage = ACCESS)
    List<JavaAnnotatedType> getAnnotatedLowerBounds();
}
