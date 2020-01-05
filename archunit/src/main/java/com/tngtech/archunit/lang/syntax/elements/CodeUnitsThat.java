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

import java.util.List;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface CodeUnitsThat<CONJUNCTION extends GivenCodeUnitsConjunction<?>> extends MembersThat<CONJUNCTION> {

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
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#haveRawParameterTypes(Class[]) haveRawParameterTypes(String.class, int.class)}</code></pre>
     *
     * @param parameterTypes Parameter types to match {@link JavaCodeUnit JavaCodeUnits} against
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawParameterTypes(Class<?>... parameterTypes);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that do not have the specified raw parameter types.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod(String stringParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#doNotHaveRawParameterTypes(Class[]) doNotHaveRawParameterTypes(String.class)}</code></pre>
     *
     * @param parameterTypes Parameter types matching {@link JavaCodeUnit JavaCodeUnits} may not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveRawParameterTypes(Class<?>... parameterTypes);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that have the specified fully qualified raw parameter type names.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod(String stringParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#haveRawParameterTypes(String[]) haveRawParameterTypes(String.class.getName(), int.class.getName())}</code></pre>
     *
     * @param parameterTypeNames Fully qualified names of parameter types to match {@link JavaCodeUnit JavaCodeUnits} against
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawParameterTypes(String... parameterTypeNames);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that do not have the specified fully qualified raw parameter type names.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod(String stringParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#doNotHaveRawParameterTypes(String[]) doNotHaveRawParameterTypes(String.class.getName())}</code></pre>
     *
     * @param parameterTypeNames Fully qualified names of parameter types matching {@link JavaCodeUnit JavaCodeUnits} may not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveRawParameterTypes(String... parameterTypeNames);

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
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#haveRawParameterTypes(DescribedPredicate) haveRawParameterTypes(whereFirstTypeIs(String.class))}</code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} match by their raw parameter types
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawParameterTypes(DescribedPredicate<? super List<JavaClass>> predicate);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that do not have raw parameter types matching the given predicate.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod(String stringParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#doNotHaveRawParameterTypes(DescribedPredicate) doNotHaveRawParameterTypes(whereFirstTypeIs(int.class))}</code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} do not match by their raw parameter types
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveRawParameterTypes(DescribedPredicate<? super List<JavaClass>> predicate);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that have the specified raw return types.
     * Take for example
     * <pre><code>
     * class Example {
     *     String someMethod() {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#haveRawReturnType(Class) haveRawReturnType(String.class)}</code></pre>
     *
     * @param type Return type to match {@link JavaCodeUnit JavaCodeUnits} against
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawReturnType(Class<?> type);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that do not have the specified raw return types.
     * Take for example
     * <pre><code>
     * class Example {
     *     String someMethod() {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#doNotHaveRawReturnType(Class) doNotHaveRawReturnType(Object.class)}</code></pre>
     *
     * @param type Return type matching {@link JavaCodeUnit JavaCodeUnits} may not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveRawReturnType(Class<?> type);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that have the specified fully qualified raw return type name.
     * Take for example
     * <pre><code>
     * class Example {
     *     String someMethod() {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#haveRawReturnType(String) haveRawReturnType(String.class.getName())}</code></pre>
     *
     * @param typeName Fully qualified name of a return type to match {@link JavaCodeUnit JavaCodeUnits} against
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawReturnType(String typeName);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that do not have the specified fully qualified raw return type name.
     * Take for example
     * <pre><code>
     * class Example {
     *     String someMethod() {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#doNotHaveRawReturnType(String) doNotHaveRawReturnType(Object.class.getName())}</code></pre>
     *
     * @param typeName Fully qualified name of a return type matching {@link JavaCodeUnit JavaCodeUnits} may not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveRawReturnType(String typeName);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that have raw return types matching the given predicate.
     * Take for example
     * <pre><code>
     * class Example {
     *     String someMethod() {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#haveRawReturnType(DescribedPredicate) haveRawReturnType(assignableTo(Serializable.class))}</code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} match by their raw return types
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawReturnType(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that do not have raw return types matching the given predicate.
     * Take for example
     * <pre><code>
     * class Example {
     *     String someMethod() {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#doNotHaveRawReturnType(DescribedPredicate) doNotHaveRawReturnType(assignableTo(List.class))}</code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} do not match by their raw return types
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveRawReturnType(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that declare a {@link Throwable} of the specified type in their throws clause.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod() throws FirstException, SecondException {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#declareThrowableOfType(Class) declareThrowableOfType(FirstException.class)}</code></pre>
     *
     * @param type Type of a declared {@link Throwable} to match {@link JavaCodeUnit JavaCodeUnits} against
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION declareThrowableOfType(Class<? extends Throwable> type);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that do not declare a {@link Throwable} of the specified type in their throws clause.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod() throws FirstException {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#doNotDeclareThrowableOfType(Class) doNotDeclareThrowableOfType(SecondException.class)}</code></pre>
     *
     * @param type Type of a declared {@link Throwable} matching {@link JavaCodeUnit JavaCodeUnits} may not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotDeclareThrowableOfType(Class<? extends Throwable> type);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that declare a {@link Throwable} of the specified fully qualified type name in their throws clause.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod() throws FirstException, SecondException {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#declareThrowableOfType(String) declareThrowableOfType(FirstException.class.getName())}</code></pre>
     *
     * @param typeName Fully qualified name of a type of a declared {@link Throwable} to match {@link JavaCodeUnit JavaCodeUnits} against
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION declareThrowableOfType(String typeName);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that do not declare a {@link Throwable} of the specified fully qualified type name in their throws clause.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod() throws FirstException {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#doNotDeclareThrowableOfType(String) doNotDeclareThrowableOfType(SecondException.class.getName())}</code></pre>
     *
     * @param typeName Fully qualified name of a type of a declared {@link Throwable} matching {@link JavaCodeUnit JavaCodeUnits} may not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotDeclareThrowableOfType(String typeName);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that declare a {@link Throwable} which matches the given predicate.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod() throws FirstException, SecondException {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#declareThrowableOfType(DescribedPredicate) declareThrowableOfType(nameStartingWith("First"))}</code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} match by their declared {@link Throwable}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION declareThrowableOfType(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches {@link JavaCodeUnit JavaCodeUnits} that do not declare a {@link Throwable} which matches the given predicate.
     * Take for example
     * <pre><code>
     * class Example {
     *     void someMethod() throws FirstException {...}
     * }
     * </code></pre>
     *
     * Then <code>someMethod</code> would be matched by
     *
     * <pre><code>{@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#that() that()}.{@link CodeUnitsThat#doNotDeclareThrowableOfType(DescribedPredicate) doNotDeclareThrowableOfType(nameStartingWith("Second"))}</code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} do not match by their declared {@link Throwable}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotDeclareThrowableOfType(DescribedPredicate<? super JavaClass> predicate);
}
