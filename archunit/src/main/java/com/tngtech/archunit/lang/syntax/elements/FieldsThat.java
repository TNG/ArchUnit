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

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface FieldsThat<CONJUNCTION extends GivenFieldsConjunction> extends MembersThat<CONJUNCTION> {

    /**
     * Matches fields by their type. Take for example
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }
     * </code></pre>
     *
     * Then <code>someField</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#that() that()}.{@link FieldsThat#haveType(Class) haveType(String.class)}</code></pre>
     *
     * @param type Type matching fields must have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveType(Class<?> type);

    /**
     * Matches fields by the fully qualified name of their type. Take for example
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }
     * </code></pre>
     *
     * Then <code>someField</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#that() that()}.{@link FieldsThat#haveType(String) haveType(String.class.getName())}</code></pre>
     *
     * @param typeName Name of type matching fields must have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveType(String typeName);

    /**
     * Matches fields where the type of those fields matches the given predicate. Take for example
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }
     * </code></pre>
     *
     * Then <code>someField</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#that() that()}.{@link FieldsThat#haveType(DescribedPredicate) haveType(assignableTo(Serializable.class))}</code></pre>
     *
     * @param predicate A predicate determining which types of fields match
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveType(DescribedPredicate<? super JavaClass> predicate);
}
