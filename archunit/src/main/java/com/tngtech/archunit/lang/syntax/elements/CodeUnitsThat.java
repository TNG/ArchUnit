/*
 * Copyright 2019 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang.syntax.elements;

import java.util.List;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface CodeUnitsThat<CONJUNCTION> extends MembersThat<CONJUNCTION> {

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that have the specified raw parameter types.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod(String stringParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#haveRawParameterTypes(Class[])  haveRawParameterTypes(String.class, int.class)}</code></pre>
     *
     * @param parameterTypes Types to match {@link JavaCodeUnit JavaCodeUnits} against
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawParameterTypes(Class<?>... parameterTypes);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that have the specified raw parameter type names.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod(String stringParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#haveRawParameterTypes(String[])  haveRawParameterTypes(String.class.getName(), int.class.getName())}</code></pre>
     *
     * @param parameterTypeNames Fully qualified names of parameter types to match {@link JavaCodeUnit JavaCodeUnits} against
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawParameterTypes(String... parameterTypeNames);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that have raw parameter types matching the given predicate.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod(String stringParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#haveRawParameterTypes(DescribedPredicate)  haveRawParameterTypes(whereFirstTypeIs(String.class))}</code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} match by their raw parameter types
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawParameterTypes(DescribedPredicate<List<JavaClass>> predicate);
}
