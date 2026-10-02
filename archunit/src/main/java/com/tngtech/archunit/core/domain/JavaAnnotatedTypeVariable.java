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
 * {@link JavaAnnotatedType} representing a usage of a {@link JavaTypeVariable}.</br>
 * If you want to get annotations on the type variable itself or bounds on the type variable use {@link JavaTypeVariable#getAnnotations()} or {@link JavaTypeVariable#getAnnotatedUpperBounds()} on the return value of {@link #getType()}.
 */
public interface JavaAnnotatedTypeVariable extends JavaAnnotatedType {

    @Override
    @PublicAPI(usage = ACCESS)
    JavaTypeVariable<?> getType();
}
