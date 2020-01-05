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
import com.tngtech.archunit.lang.ArchRule;
import org.junit.platform.commons.annotation.Testable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks ArchUnit tests to be executed by the test infrastructure. These tests can have the following form:
 * <ul>
 *     <li>
 *         A static field of type {@link ArchRule} -&gt; this rule will automatically be checked against the imported classes
 *     </li>
 *     <li>
 *         A static method with one parameter {@link JavaClasses} -&gt; this method will be called with the imported classes
 *     </li>
 * </ul>
 * <br>Example:
 * <pre><code>
 *{@literal @}ArchTest
 * public static final ArchRule someRule = classes()... ;
 *
 *{@literal @}ArchTest
 * public static void someMethod(JavaClasses classes) {
 *     // do something with classes
 * }
 * </code></pre>
 */
@Testable
@Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface ArchTest {
}
