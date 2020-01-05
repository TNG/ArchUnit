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
package com.tngtech.archunit.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeJars;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.core.importer.ImportOptions;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies which packages/locations should be scanned and tested when running a test with the {@link ArchUnitRunner}.
 * <br><br>
 * To ignore certain classes (e.g. classes in test scope) see {@link #importOptions()}, in particular {@link DoNotIncludeTests} and
 * {@link DoNotIncludeJars}.
 * <br><br>
 * When checking rules, it is important to remember that all relevant information/classes need to be imported for the rules
 * to work. For example, if class A accesses class B and class B extends class C, but class B is not imported, then
 * a rule checking for no accesses to classes assignable to C will not fail, since ArchUnit does not know about the details
 * of class B, but only simple information like the fully qualified name. For information how to configure the import and
 * resolution behavior of missing classes, compare {@link ClassFileImporter}.
 *
 * @see ArchUnitRunner
 * @see ClassFileImporter
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface AnalyzeClasses {
    /**
     * @return Packages to look for within the classpath / modulepath
     */
    String[] packages() default {};

    /**
     * @return Classes that specify packages to look for within the classpath / modulepath
     */
    Class<?>[] packagesOf() default {};

    /**
     * @return Implementations of {@link LocationProvider}. Allows to completely customize the sources,
     * where classes are imported from.
     */
    Class<? extends LocationProvider>[] locations() default {};

    /**
     * Allows to filter the class import. The supplied types will be instantiated and used to create the
     * {@link ImportOptions} passed to the {@link ClassFileImporter}. Considering caching, compare the notes on
     * {@link ImportOption}.
     *
     * @return The types of {@link ImportOption} to use for the import
     */
    Class<? extends ImportOption>[] importOptions() default {};

    /**
     * Controls, if {@link JavaClasses} should be cached by location,
     * to be reused between several test classes, or just within the same class.
     *
     * @return The {@link CacheMode} to use for this test class.
     */
    CacheMode cacheMode() default CacheMode.FOREVER;
}
