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

import com.tngtech.archunit.Internal;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Simply a container for {@link ArchTag}. Should never be used directly, but instead
 * {@link ArchTag} should be used in a {@linkplain Repeatable repeatable} manner, e.g.
 * <br><br>
 * <pre><code>
 *{@literal @}ArchTag("foo")
 *{@literal @}ArchTag("bar")
 * static ArchRule example = classes()...
 * </code></pre>
 */
@Internal
@Inherited
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD, FIELD})
public @interface ArchTags {
    ArchTag[] value();
}
