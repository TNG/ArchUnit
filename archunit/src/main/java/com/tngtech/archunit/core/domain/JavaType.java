/*
 * Copyright 2014-2024 TNG Technology Consulting GmbH
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

import java.lang.reflect.Type;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.core.domain.properties.HasName;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Represents a general Java type. This can e.g. be a class like {@code java.lang.String}, a parameterized type
 * like {@code List<String>} or a type variable like {@code T}.<br>
 * Besides having a {@link HasName#getName() name} and offering the possibility to being converted to an
 * {@link #toErasure() erasure} (which is then always {@link JavaClass a raw class object}) {@link JavaType} doesn't offer
 * an extensive API. Instead, users can check a {@link JavaType} for being an instance of a concrete subtype
 * (like {@link JavaTypeVariable}) and then cast it to the respective subclass
 * (same as with {@link Type} of the Java Reflection API).
 *
 * @see JavaClass
 * @see JavaParameterizedType
 * @see JavaTypeVariable
 * @see JavaWildcardType
 * @see JavaGenericArrayType
 */
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

    /**
     * Predefined {@link ChainableFunction functions} to transform {@link JavaType}.
     */
    @PublicAPI(usage = ACCESS)
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
