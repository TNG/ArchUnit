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
package com.tngtech.archunit.core;

import java.util.Set;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * Can be implemented to express that this object might also be considered as object(s) of a different type.
 * E.g. {@link JavaAccess} and {@link Dependency} (compare {@link #convertTo(Class)}).
 */
@PublicAPI(usage = INHERITANCE)
public interface Convertible {
    /**
     * Converts this type to a set of other types.
     * For example a {@link JavaAccess} can also be
     * considered a {@link Dependency}, so <code>javaAccess.convertTo(Dependency.class)</code>
     * will yield a set with a single {@link Dependency} representing this access.
     * Or a component dependency grouping many class dependencies could be considered a set of exactly
     * these class dependencies.
     * The result will be an empty set if no conversion is possible
     * (e.g. calling <code>javaAccess.convertTo(Integer.class)</code>.
     *
     * @param type The type to convert to
     * @return A set of converted elements, empty if no conversion is possible
     */
    <T> Set<T> convertTo(Class<T> type);
}
