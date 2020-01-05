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

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link ArchTag @ArchTag} is a {@linkplain Repeatable repeatable} annotation that allows
 * tagging any {@link ArchTest @ArchTest} field/method/class. Sets of rules can be classified to run together this way.
 *
 * Rules could be tagged like
 * <br><br>
 * <pre><code>
 *{@literal @}ArchTag("dependencies")
 * static ArchRule no_accesses_from_server_to_client = classes()...
 * </code></pre>
 *
 * Users of {@link ArchTag} must follow the syntax conventions that the JUnit Platform Engine defines:
 *
 * <ul>
 * <li>A tag must not be blank.</li>
 * <li>A <em>trimmed</em> tag must not contain whitespace.</li>
 * <li>A <em>trimmed</em> tag must not contain ISO control characters.</li>
 * <li>A <em>trimmed</em> tag must not contain any of the following
 * <em>reserved characters</em>.
 * <ul>
 * <li>{@code ,}: <em>comma</em></li>
 * <li>{@code (}: <em>left parenthesis</em></li>
 * <li>{@code )}: <em>right parenthesis</em></li>
 * <li>{@code &}: <em>ampersand</em></li>
 * <li>{@code |}: <em>vertical bar</em></li>
 * <li>{@code !}: <em>exclamation point</em></li>
 * </ul>
 * </li>
 * </ul>
 */
@Inherited
@Documented
@Retention(RUNTIME)
@PublicAPI(usage = ACCESS)
@Repeatable(ArchTags.class)
@Target({TYPE, METHOD, FIELD})
public @interface ArchTag {
    /**
     * The actual <em>tag</em>. It will first be {@linkplain String#trim() trimmed} and must then adhere to the
     * {@linkplain ArchTag Syntax Rules for Tags}. Otherwise the tag will be ignored.
     */
    String value();
}
