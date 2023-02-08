/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.base;

import java.lang.annotation.Retention;

import com.tngtech.archunit.Internal;

import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Marks the methods or classes that resolve types via reflection so we can use it for architecture
 * assertions.
 */
@Retention(CLASS)
@Internal
public @interface ResolvesTypesViaReflection {
}
