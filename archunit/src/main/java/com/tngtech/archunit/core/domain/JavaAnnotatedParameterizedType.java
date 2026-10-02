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

import java.util.List;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * {@link JavaAnnotatedType} representing a usage of type with type parameters.</br>
 * The parameters can be obtained with annotations from {@link #getAnnotatedActualTypeArguments()}.
 */
@PublicAPI(usage = ACCESS)
public interface JavaAnnotatedParameterizedType extends JavaAnnotatedType {

    @Override
    @PublicAPI(usage = ACCESS)
    JavaParameterizedType getType();

    /**
     *
     * @return {@code JavaAnnotatedType}s used as type parameters of this parameterized type, e.g. {@code List.of(@A2 String, @A3 )} from {@code @A1 Map<@A2 String, @A3 Object>}.
     */
    @PublicAPI(usage = ACCESS)
    List<JavaAnnotatedType> getAnnotatedActualTypeArguments();

}
