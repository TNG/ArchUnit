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

public interface CodeUnitsShould<CONJUNCTION extends CodeUnitsShouldConjunction<?>> extends MembersShould<CONJUNCTION> {

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} have the specified raw parameter types.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#haveRawParameterTypes(Class[]) haveRawParameterTypes(String.class, int.class)}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod(Object wrongParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * @param parameterTypes Parameter types {@link JavaCodeUnit JavaCodeUnits} should have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawParameterTypes(Class<?>... parameterTypes);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} do not have the specified raw parameter types.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#notHaveRawParameterTypes(Class[]) notHaveRawParameterTypes(String.class, int.class)}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod(String stringParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * @param parameterTypes Parameter types {@link JavaCodeUnit JavaCodeUnits} should not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveRawParameterTypes(Class<?>... parameterTypes);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} have the specified fully qualified raw parameter type names.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#haveRawParameterTypes(String[]) haveRawParameterTypes(String.class.getName(), int.class.getName())}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod(Object wrongParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * @param parameterTypeNames Fully qualified names of parameter types {@link JavaCodeUnit JavaCodeUnits} should have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawParameterTypes(String... parameterTypeNames);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} do not have the specified fully qualified raw parameter type names.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#notHaveRawParameterTypes(String[]) notHaveRawParameterTypes(String.class.getName(), int.class.getName())}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod(String stringParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * @param parameterTypeNames Fully qualified names of parameter types {@link JavaCodeUnit JavaCodeUnits} should not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveRawParameterTypes(String... parameterTypeNames);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} have raw parameter types matching the given predicate.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#haveRawParameterTypes(DescribedPredicate) haveRawParameterTypes(whereFirstTypeIs(String.class))}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod(Object wrongParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} match by their raw parameter types
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawParameterTypes(DescribedPredicate<? super List<JavaClass>> predicate);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} do not have raw parameter types matching the given predicate.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#notHaveRawParameterTypes(DescribedPredicate) notHaveRawParameterTypes(whereFirstTypeIs(String.class))}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod(String stringParam, int intParam) {...}
     * }
     * </code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} do not match by their raw parameter types
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveRawParameterTypes(DescribedPredicate<? super List<JavaClass>> predicate);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} have the specified raw return type.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#haveRawReturnType(Class) haveRawReturnType(String.class)}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someMethod() {...}
     * }
     * </code></pre>
     *
     * @param type Return type {@link JavaCodeUnit JavaCodeUnits} should have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawReturnType(Class<?> type);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} do not have the specified raw return type.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#notHaveRawReturnType(Class) notHaveRawReturnType(String.class)}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     String someMethod() {...}
     * }
     * </code></pre>
     *
     * @param type Return type {@link JavaCodeUnit JavaCodeUnits} should not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveRawReturnType(Class<?> type);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} have the specified fully qualified raw return type name.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#haveRawReturnType(String) haveRawReturnType(String.class.getName())}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someMethod() {...}
     * }
     * </code></pre>
     *
     * @param typeName Fully qualified name of a return type {@link JavaCodeUnit JavaCodeUnits} should have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawReturnType(String typeName);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} do not have the specified fully qualified raw return type name.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#notHaveRawReturnType(String) notHaveRawReturnType(String.class.getName())}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     String someMethod() {...}
     * }
     * </code></pre>
     *
     * @param typeName Fully qualified name of a return type {@link JavaCodeUnit JavaCodeUnits} should not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveRawReturnType(String typeName);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} have raw return types matching the given predicate.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#haveRawReturnType(DescribedPredicate) haveRawReturnType(assignableTo(Serializable.class))}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someMethod() {...}
     * }
     * </code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} match by their raw return types
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveRawReturnType(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} do not have raw return types matching the given predicate.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#notHaveRawReturnType(DescribedPredicate) notHaveRawReturnType(assignableTo(Serializable.class))}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     String someMethod() {...}
     * }
     * </code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} do not match by their raw return types
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveRawReturnType(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} declare a {@link Throwable} of the specified type in their throws clause.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#declareThrowableOfType(Class) declareThrowableOfType(SomeException.class)}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod() throws WrongException {...}
     * }
     * </code></pre>
     *
     * @param type Type of a declared {@link Throwable} {@link JavaCodeUnit JavaCodeUnits} should have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION declareThrowableOfType(Class<? extends Throwable> type);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} do not declare a {@link Throwable} of the specified type in their throws clause.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#notDeclareThrowableOfType(Class) notDeclareThrowableOfType(SomeException.class)}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod() throws SomeException {...}
     * }
     * </code></pre>
     *
     * @param type Type of a declared {@link Throwable} {@link JavaCodeUnit JavaCodeUnits} should not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notDeclareThrowableOfType(Class<? extends Throwable> type);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} declare a {@link Throwable} of the specified fully qualified type name in their throws clause.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#declareThrowableOfType(String) declareThrowableOfType(SomeException.class.getName())}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod() throws WrongException {...}
     * }
     * </code></pre>
     *
     * @param typeName Fully qualified name of a type of a declared {@link Throwable} {@link JavaCodeUnit JavaCodeUnits} should have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION declareThrowableOfType(String typeName);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} do not declare a {@link Throwable} of the specified fully qualified type name in their throws clause.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#notDeclareThrowableOfType(String) notDeclareThrowableOfType(SomeException.class.getName())}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod() throws SomeException {...}
     * }
     * </code></pre>
     *
     * @param typeName Fully qualified name of a type of a declared {@link Throwable} {@link JavaCodeUnit JavaCodeUnits} should not have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notDeclareThrowableOfType(String typeName);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} declare a {@link Throwable} which matches the given predicate.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#declareThrowableOfType(DescribedPredicate) declareThrowableOfType(nameStartingWith("Some"))}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod() throws WrongException {...}
     * }
     * </code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} match by their declared {@link Throwable}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION declareThrowableOfType(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Asserts that {@link JavaCodeUnit JavaCodeUnits} do not declare a {@link Throwable} which matches the given predicate.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#codeUnits() codeUnits()}.{@link GivenCodeUnits#should() should()}.{@link CodeUnitsShould#notDeclareThrowableOfType(DescribedPredicate) notDeclareThrowableOfType(nameStartingWith("Some"))}
     * </code></pre>
     * would be violated by <code>someMethod</code> in
     *
     * <pre><code>
     * class Example {
     *     void someMethod() throws SomeException {...}
     * }
     * </code></pre>
     *
     * @param predicate A {@link DescribedPredicate} that determines, which {@link JavaCodeUnit JavaCodeUnits} do not match by their declared {@link Throwable}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notDeclareThrowableOfType(DescribedPredicate<? super JavaClass> predicate);
}
