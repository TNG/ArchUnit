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

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public interface JavaType extends HasName {
    /**
     * Converts this {@link JavaType} into the erased type
     * (compare the <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.6"> Java Language Specification</a>).
     * In particular this will result in
     * <ul>
     *     <li>the class itself, if this type is a {@link JavaClass}</li>
     *     <li>the {@link JavaClass} equivalent to {@link Object}, if this type is an unbound {@link JavaTypeVariable}</li>
     *     <li>the {@link JavaClass} equivalent to the erasure of the left most bound, if this type is a bound {@link JavaTypeVariable}</li>
     *     <li>if this type is a {@link JavaGenericArrayType}, the erasure will be the {@link JavaClass}
     *     equivalent to the array type that has the erasure of the generic component type of this type as its component type;
     *     e.g. take the generic array type {@code T[][]} where {@code T} is unbound, then the erasure will be the array type {@code Object[][]}</li>
     * </ul>
     */
    @PublicAPI(usage = ACCESS)
    JavaClass toErasure();

    final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<JavaType, JavaClass> TO_ERASURE = new ChainableFunction<JavaType, JavaClass>() {
            @Override
            public JavaClass apply(JavaType input) {
                return input.toErasure();
            }
        };
    }
}
