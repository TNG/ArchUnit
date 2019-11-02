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

import java.lang.annotation.Annotation;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface MembersShould<CONJUNCTION extends MembersShouldConjunction<?>> {

    /**
     * Asserts that members have a certain name (i.e. field name, method name or {@link JavaConstructor#CONSTRUCTOR_NAME}).
     *
     * @param name The member name
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveName(String name);

    /**
     * Asserts that members do not have a certain name (i.e. field name, method name or {@link JavaConstructor#CONSTRUCTOR_NAME}).
     *
     * @param name The member name
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveName(String name);

    /**
     * Asserts that members have a name (i.e. field name, method name or {@link JavaConstructor#CONSTRUCTOR_NAME})
     * matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveNameMatching(String regex);

    /**
     * Asserts that members have a name (i.e. field name, method name or {@link JavaConstructor#CONSTRUCTOR_NAME})
     * not matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveNameNotMatching(String regex);

    /**
     * Asserts that members have a certain full name (compare {@link JavaField#getFullName()} and {@link JavaCodeUnit#getFullName()}).
     *
     * @param fullName The member's full name
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveFullName(String fullName);

    /**
     * Asserts that members do not have a certain full name (compare {@link JavaField#getFullName()} and {@link JavaCodeUnit#getFullName()}).
     *
     * @param fullName The member's full name
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveFullName(String fullName);

    /**
     * Asserts that members have a full name matching a given regular expression (compare {@link JavaField#getFullName()}
     * and {@link JavaCodeUnit#getFullName()}).
     *
     * @param regex A regular expression
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveFullNameMatching(String regex);

    /**
     * Asserts that members have a full name not matching a given regular expression (compare {@link JavaField#getFullName()}
     * and {@link JavaCodeUnit#getFullName()}).
     *
     * @param regex A regular expression
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveFullNameNotMatching(String regex);

    /**
     * Asserts that members are public.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION bePublic();

    /**
     * Asserts that members are non-public.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBePublic();

    /**
     * Asserts that members are protected.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beProtected();

    /**
     * Asserts that members are non-protected.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeProtected();

    /**
     * Asserts that members are package private.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION bePackagePrivate();

    /**
     * Asserts that members are non-package private.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBePackagePrivate();

    /**
     * Asserts that members are private.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION bePrivate();

    /**
     * Asserts that members are non-private.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBePrivate();

    /**
     * Asserts that members have a certain {@link JavaModifier} (e.g. {@link JavaModifier#ABSTRACT}).
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveModifier(JavaModifier modifier);

    /**
     * Asserts that members do not have a certain {@link JavaModifier} (e.g. {@link JavaModifier#ABSTRACT}).
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notHaveModifier(JavaModifier modifier);

    /**
     * Asserts that members are annotated with a certain type of annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Asserts that members are not annotated with a certain type of annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Asserts that members are annotated with a certain type of annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beAnnotatedWith(String annotationTypeName);

    /**
     * Asserts that members are not annotated with a certain type of annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeAnnotatedWith(String annotationTypeName);

    /**
     * Asserts that members are annotated with an annotation matching the supplied predicate.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Asserts that members are not annotated with an annotation matching the supplied predicate.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Asserts that members are meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beMetaAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Asserts that members are not meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeMetaAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Asserts that members are meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beMetaAnnotatedWith(String annotationTypeName);

    /**
     * Asserts that members are not meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeMetaAnnotatedWith(String annotationTypeName);

    /**
     * Asserts that members are meta-annotated with an annotation matching the supplied predicate.
     * A meta-annotation is an annotation that is declared on another annotation.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Asserts that members are not meta-annotated with an annotation matching the supplied predicate.
     * A meta-annotation is an annotation that is declared on another annotation.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Asserts that members are declared within the supplied class.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#should() should()}.{@link MembersShould#beDeclaredIn(Class) beDeclaredIn(Example.class)}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class AnyOther {
     *     Object someField;
     * }</code></pre>
     *
     * @param javaClass A class that members should be declared in
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beDeclaredIn(Class<?> javaClass);

    /**
     * Asserts that members are not declared within the supplied class.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#should() should()}.{@link MembersShould#notBeDeclaredIn(Class) notBeDeclaredIn(Example.class)}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * @param javaClass A class that members should not be declared in
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeDeclaredIn(Class<?> javaClass);

    /**
     * Asserts that members are declared within a class of the supplied class name.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#should() should()}.{@link MembersShould#beDeclaredIn(String) beDeclaredIn(Example.class.getName())}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class AnyOther {
     *     Object someField;
     * }</code></pre>
     *
     * @param className Fully qualified name of a class that members should be declared in
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beDeclaredIn(String className);

    /**
     * Asserts that members are not declared within a class of the supplied class name.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#should() should()}.{@link MembersShould#notBeDeclaredIn(String) notBeDeclaredIn(Example.class.getName())}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * @param className Fully qualified name of a class that members must not be declared in to match
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION notBeDeclaredIn(String className);

    /**
     * Asserts that members are declared within a class matching the supplied predicate.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#should() should()}.{@link MembersShould#beDeclaredInClassesThat(DescribedPredicate) beDeclaredInClassesThat(areSubTypeOf(Example.class))}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class AnyOther {
     *     Object someField;
     * }</code></pre>
     *
     * @param predicate A predicate which matches classes where members have to be declared in
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION beDeclaredInClassesThat(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Allows to assert that members are declared within a certain class.
     * <br><br>
     * E.g.
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#should() should()}.{@link MembersShould#beDeclaredInClassesThat() beDeclaredInClassesThat()}.{@link ClassesThat#areAssignableTo(Class) areAssignableTo(Example.class)}
     * </code></pre>
     * would be violated by <code>someField</code> in
     *
     * <pre><code>
     * class AnyOther {
     *     Object someField;
     * }</code></pre>
     *
     * @return A syntax element that allows restricting where members are declared in
     */
    @PublicAPI(usage = ACCESS)
    ClassesThat<CONJUNCTION> beDeclaredInClassesThat();
}
