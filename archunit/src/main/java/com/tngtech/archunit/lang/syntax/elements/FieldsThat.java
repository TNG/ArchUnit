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
package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface FieldsThat<CONJUNCTION extends GivenFieldsConjunction> extends MembersThat<CONJUNCTION> {

    /**
     * Matches fields by their raw type. Take for example
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }
     * </code></pre>
     *
     * Then <code>someField</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#that() that()}.{@link FieldsThat#haveRawType(Class) haveRawType(String.class)}</code></pre>
     *
     * @param type Type matching fields must have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawType(Class<?> type);

    /**
     * Matches fields that do not have the given raw type. Take for example
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }
     * </code></pre>
     *
     * Then <code>someField</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#that() that()}.{@link FieldsThat#doNotHaveRawType(Class) doNotHaveRawType(Object.class)}</code></pre>
     *
     * @param type Type matching fields must not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveRawType(Class<?> type);

    /**
     * Matches fields by the fully qualified name of their raw type. Take for example
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }
     * </code></pre>
     *
     * Then <code>someField</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#that() that()}.{@link FieldsThat#haveRawType(String) haveRawType(String.class.getName())}</code></pre>
     *
     * @param typeName Name of type matching fields must have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawType(String typeName);

    /**
     * Matches fields that do not have the given fully qualified name of their raw type. Take for example
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }
     * </code></pre>
     *
     * Then <code>someField</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#that() that()}.{@link FieldsThat#doNotHaveRawType(String) doNotHaveRawType(Object.class.getName())}</code></pre>
     *
     * @param typeName Name of type matching fields must not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveRawType(String typeName);

    /**
     * Matches fields where the raw type of those fields matches the given predicate. Take for example
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }
     * </code></pre>
     *
     * Then <code>someField</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#that() that()}.{@link FieldsThat#haveRawType(DescribedPredicate) haveRawType(assignableTo(Serializable.class))}</code></pre>
     *
     * @param predicate A predicate determining which types of fields match
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawType(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches fields where the raw type of those fields does not match the given predicate. Take for example
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }
     * </code></pre>
     *
     * Then <code>someField</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#that() that()}.{@link FieldsThat#doNotHaveRawType(DescribedPredicate) doNotHaveRawType(assignableTo(List.class))}</code></pre>
     *
     * @param predicate A predicate determining which types of fields do not match
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveRawType(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches static fields.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areStatic();

    /**
     * Matches non-static fields.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotStatic();

    /**
     * Matches final fields.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areFinal();

    /**
     * Matches non-final fields.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotFinal();
}
