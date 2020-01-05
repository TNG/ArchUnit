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

public interface FieldsShould<CONJUNCTION extends FieldsShouldConjunction> extends MembersShould<CONJUNCTION> {

    /**
     * Asserts that fields have a certain raw type.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#should() should()}.{@link FieldsShould#haveRawType(Class) haveRawType(String.class)}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * @param type Type fields should have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawType(Class<?> type);

    /**
     * Asserts that fields do not have a certain raw type.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#should() should()}.{@link FieldsShould#notHaveRawType(Class) notHaveRawType(String.class)}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }</code></pre>
     *
     * @param type Type fields should not have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveRawType(Class<?> type);

    /**
     * Asserts that fields have a certain fully qualified name of their raw type.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#should() should()}.{@link FieldsShould#haveRawType(String) haveRawType(String.class.getName())}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * @param typeName Name of type fields should have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawType(String typeName);

    /**
     * Asserts that fields do not have a certain fully qualified name of their raw type.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#should() should()}.{@link FieldsShould#notHaveRawType(String) notHaveRawType(String.class.getName())}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }</code></pre>
     *
     * @param typeName Name of type fields should not have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveRawType(String typeName);

    /**
     * Asserts that fields have a raw type matching the given predicate.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#should() should()}.{@link FieldsShould#haveRawType(DescribedPredicate) haveRawType(assignableTo(Serializable.class))}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * @param predicate A predicate determining which sort of types fields should have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawType(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Asserts that fields do not have a raw type matching the given predicate.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#fields() fields()}.{@link GivenFields#should() should()}.{@link FieldsShould#notHaveRawType(DescribedPredicate) notHaveRawType(assignableTo(Serializable.class))}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     String someField;
     * }</code></pre>
     *
     * @param predicate A predicate determining which sort of types fields should not have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveRawType(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Asserts that fields are static.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beStatic();

    /**
     * Asserts that fields are non-static.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeStatic();

    /**
     * Asserts that fields are final.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beFinal();

    /**
     * Asserts that fields are non-final.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeFinal();
}
