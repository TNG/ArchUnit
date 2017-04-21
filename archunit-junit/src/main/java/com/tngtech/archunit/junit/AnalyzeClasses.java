/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.core.importer.ImportOption;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies which packages should be scanned and tested when running a test via the {@link ArchUnitRunner}.
 *
 * @see ArchUnitRunner
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface AnalyzeClasses {
    String[] packages() default {};

    Class[] packagesOf() default {};

    Class<? extends ImportOption> importOption() default ImportOption.Everything.class;
}
