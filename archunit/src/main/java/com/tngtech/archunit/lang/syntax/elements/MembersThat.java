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
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface MembersThat<CONJUNCTION extends GivenMembersConjunction<?>> {

    /**
     * Matches members by their name (i.e. field name, method name or {@link JavaConstructor#CONSTRUCTOR_NAME}).
     *
     * @param name The member name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveName(String name);

    /**
     * Matches members that do not have a certain name (i.e. field name, method name or {@link JavaConstructor#CONSTRUCTOR_NAME}).
     *
     * @param name The member name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveName(String name);

    /**
     * Matches members with a name (i.e. field name, method name or {@link JavaConstructor#CONSTRUCTOR_NAME})
     * matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveNameMatching(String regex);

    /**
     * Matches members with a name (i.e. field name, method name or {@link JavaConstructor#CONSTRUCTOR_NAME})
     * not matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveNameNotMatching(String regex);

    /**
     * Matches members by their full name.
     *
     * @param fullName The member's full name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveFullName(String fullName);

    /**
     * Matches members that do not have a certain full name.
     *
     * @param fullName The member's full name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveFullName(String fullName);

    /**
     * Matches members with a full name matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveFullNameMatching(String regex);

    /**
     * Matches members with a full name not matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveFullNameNotMatching(String regex);

    /**
     * Matches public members.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION arePublic();

    /**
     * Matches non-public members.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotPublic();

    /**
     * Matches protected members.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areProtected();

    /**
     * Matches non-protected members.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotProtected();

    /**
     * Matches package private members.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION arePackagePrivate();

    /**
     * Matches non-package private members.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotPackagePrivate();

    /**
     * Matches private members.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION arePrivate();

    /**
     * Matches non-private members.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotPrivate();

    /**
     * Matches members having a certain {@link JavaModifier} (e.g. {@link JavaModifier#ABSTRACT}).
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveModifier(JavaModifier modifier);

    /**
     * Matches members not having a certain {@link JavaModifier} (e.g. {@link JavaModifier#ABSTRACT}).
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveModifier(JavaModifier modifier);

    /**
     * Matches members annotated with a certain type of annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Matches members not annotated with a certain type of annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Matches members annotated with a certain type of annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAnnotatedWith(String annotationTypeName);

    /**
     * Matches members not annotated with a certain type of annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAnnotatedWith(String annotationTypeName);

    /**
     * Matches members annotated with a certain annotation, where matching annotations are
     * determined by the supplied predicate.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Matches members not annotated with a certain annotation, where matching annotations are
     * determined by the supplied predicate.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Matches members meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areMetaAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Matches members not meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotMetaAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Matches members meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areMetaAnnotatedWith(String annotationTypeName);

    /**
     * Matches members not meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotMetaAnnotatedWith(String annotationTypeName);

    /**
     * Matches members meta-annotated with a certain annotation, where matching meta-annotations are
     * determined by the supplied predicate.  A meta-annotation is an annotation that is declared on
     * another annotation.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Matches members not meta-annotated with a certain annotation, where matching meta-annotations are
     * determined by the supplied predicate.  A meta-annotation is an annotation that is declared on
     * another annotation.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Matches members declared within the supplied class.
     * <br><br>
     * E.g. <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * will be matched by
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#that() that()}.{@link MembersThat#areDeclaredIn(Class) areDeclaredIn(Example.class)}
     * </code></pre>
     *
     * @param javaClass A class that members have to be declared in to match
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areDeclaredIn(Class<?> javaClass);

    /**
     * Matches members not declared within the supplied class.
     * <br><br>
     * E.g. <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * will not be matched by
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#that() that()}.{@link MembersThat#areNotDeclaredIn(Class) areNotDeclaredIn(Example.class)}
     * </code></pre>
     *
     * @param javaClass A class that members must not be declared in to match
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotDeclaredIn(Class<?> javaClass);

    /**
     * Matches members declared within a class of the supplied class name.
     * <br><br>
     * E.g. <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * will be matched by
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#that() that()}.{@link MembersThat#areDeclaredIn(String) areDeclaredIn(Example.class.getName())}
     * </code></pre>
     *
     * @param className Fully qualified name of a class that members have to be declared in to match
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areDeclaredIn(String className);

    /**
     * Matches members not declared within a class of the supplied class name.
     * <br><br>
     * E.g. <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * will not be matched by
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#that() that()}.{@link MembersThat#areNotDeclaredIn(String) areNotDeclaredIn(Example.class.getName())}
     * </code></pre>
     *
     * @param className Fully qualified name of a class that members must not be declared in to match
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotDeclaredIn(String className);

    /**
     * Matches members declared within a class matching the supplied predicate.
     * <br><br>
     * E.g. <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * will be matched by
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#that() that()}.{@link MembersThat#areDeclaredInClassesThat(DescribedPredicate) areDeclaredInClassesThat(areSubTypeOf(Object.class))}
     * </code></pre>
     *
     * @param predicate A predicate which matches classes where members have to be declared in
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areDeclaredInClassesThat(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Allows to restrict the set of members under consideration by the class they are declared in
     * <br><br>
     * E.g. <code>someField</code> in
     *
     * <pre><code>
     * class Example {
     *     Object someField;
     * }</code></pre>
     *
     * will be matched by
     * <pre><code>
     * {@link ArchRuleDefinition#members() members()}.{@link GivenMembers#that() that()}.{@link MembersThat#areDeclaredInClassesThat() areDeclaredInClassesThat()}.{@link ClassesThat#areAssignableTo(Class) areAssignableTo(Object.class)}
     * </code></pre>
     *
     * @return A syntax element that allows restricting where members are declared in
     */
    @PublicAPI(usage = ACCESS)
    ClassesThat<CONJUNCTION> areDeclaredInClassesThat();
}
