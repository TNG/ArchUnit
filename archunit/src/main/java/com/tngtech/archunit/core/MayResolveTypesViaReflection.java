/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import java.lang.annotation.Retention;

import com.tngtech.archunit.Internal;

import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Marks the methods or classes, where resolving types via reflection is allowed.
 * These should be carefully controlled, because the import of classes may not rely on class loading
 * (while resolving dependencies might be done using the classpath), and neither should
 * the predefined conditions and predicates.
 */
@Retention(CLASS)
@Internal
public @interface MayResolveTypesViaReflection {
    String reason();
}
