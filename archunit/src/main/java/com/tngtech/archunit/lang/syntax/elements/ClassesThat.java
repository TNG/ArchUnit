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
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.HasName.Predicates;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface ClassesThat<CONJUNCTION> {
    /**
     * Matches classes by their fully qualified class name.
     *
     * @param name The fully qualified class name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveFullyQualifiedName(String name);

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doNotHaveFullyQualifiedName(String)}
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    CONJUNCTION dontHaveFullyQualifiedName(String name);

    /**
     * Matches classes that do not have a certain fully qualified class name.
     *
     * @param name The fully qualified class name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveFullyQualifiedName(String name);

    /**
     * Matches classes by their simple class name.
     *
     * @param name The simple class name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveSimpleName(String name);

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doNotHaveSimpleName(String)}
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    CONJUNCTION dontHaveSimpleName(String name);

    /**
     * Matches classes that do not have a certain simple class name.
     *
     * @param name The simple class name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveSimpleName(String name);

    /**
     * Matches classes with a fully qualified class name matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveNameMatching(String regex);

    /**
     * Matches classes with a fully qualified class name not matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveNameNotMatching(String regex);

    /**
     * Matches classes with a simple class name starting with a given prefix.
     *
     * @param prefix A prefix the simple class name should start with
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveSimpleNameStartingWith(String prefix);

    /**
     * Matches classes with a simple class name not starting with a given prefix.
     *
     * @param prefix A prefix the simple class name should not start with
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveSimpleNameNotStartingWith(String prefix);

    /**
     * Matches classes with a simple class name containing the specified infix.
     *
     * @param infix An infix the simple class name should contain
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveSimpleNameContaining(String infix);

    /**
     * Matches classes with a simple class name not containing the specified infix.
     *
     * @param infix An infix the simple class name should not contain
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveSimpleNameNotContaining(String infix);

    /**
     * Matches classes with a simple class name ending with a given suffix.
     *
     * @param suffix A suffix the simple class name should end with
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveSimpleNameEndingWith(String suffix);

    /**
     * Matches classes with a simple class name not ending with a given suffix.
     *
     * @param suffix A suffix the simple class name should not end with
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveSimpleNameNotEndingWith(String suffix);

    /**
     * Matches classes residing in a package matching the supplied package identifier.
     *
     * @param packageIdentifier A string identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION resideInAPackage(String packageIdentifier);

    /**
     * Matches classes residing in a package matching any of the supplied package identifiers.
     *
     * @param packageIdentifiers Strings identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION resideInAnyPackage(String... packageIdentifiers);

    /**
     * Matches classes not residing in a package matching the supplied package identifier.
     *
     * @param packageIdentifier A string identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION resideOutsideOfPackage(String packageIdentifier);

    /**
     * Matches classes not residing in a package matching any of the supplied package identifiers.
     *
     * @param packageIdentifiers Strings identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION resideOutsideOfPackages(String... packageIdentifiers);

    /**
     * Matches public classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION arePublic();

    /**
     * Matches non-public classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotPublic();

    /**
     * Matches protected classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areProtected();

    /**
     * Matches non-protected classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotProtected();

    /**
     * Matches package private classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION arePackagePrivate();

    /**
     * Matches non-package private classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotPackagePrivate();

    /**
     * Matches private classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION arePrivate();

    /**
     * Matches non-private classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotPrivate();

    /**
     * Matches classes having a certain {@link JavaModifier} (e.g. {@link JavaModifier#ABSTRACT}).
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION haveModifier(JavaModifier modifier);

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doNotHaveModifier(JavaModifier)}
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    CONJUNCTION dontHaveModifier(JavaModifier modifier);

    /**
     * Matches classes not having a certain {@link JavaModifier} (e.g. {@link JavaModifier#ABSTRACT}).
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotHaveModifier(JavaModifier modifier);

    /**
     * Matches classes annotated with a certain type of annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Matches classes not annotated with a certain type of annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Matches classes annotated with a certain type of annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAnnotatedWith(String annotationTypeName);

    /**
     * Matches classes not annotated with a certain type of annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAnnotatedWith(String annotationTypeName);

    /**
     * Matches classes annotated with a certain annotation, where matching annotations are
     * determined by the supplied predicate.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Matches classes not annotated with a certain annotation, where matching annotations are
     * determined by the supplied predicate.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Matches classes meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areMetaAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Matches classes not meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotMetaAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Matches classes meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areMetaAnnotatedWith(String annotationTypeName);

    /**
     * Matches classes not meta-annotated with a certain type of annotation. A meta-annotation is
     * an annotation that is declared on another annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotMetaAnnotatedWith(String annotationTypeName);

    /**
     * Matches classes meta-annotated with a certain annotation, where matching meta-annotations are
     * determined by the supplied predicate.  A meta-annotation is an annotation that is declared on
     * another annotation.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Matches classes not meta-annotated with a certain annotation, where matching meta-annotations are
     * determined by the supplied predicate.  A meta-annotation is an annotation that is declared on
     * another annotation.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotMetaAnnotatedWith(DescribedPredicate<? super JavaAnnotation<?>> predicate);

    /**
     * Matches classes that implement a certain interface.
     *
     * @param type An interface type matching classes must implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION implement(Class<?> type);

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doNotImplement(Class)}
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    CONJUNCTION dontImplement(Class<?> type);

    /**
     * Matches classes that do not implement a certain interface. This is the negation of {@link #implement(Class)}.
     *
     * @param type An interface type matching classes must not implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotImplement(Class<?> type);

    /**
     * Matches classes that implement a certain interface with the given type name. This is equivalent to
     * {@link #implement(Class)}, but does not depend on having a certain type on the classpath.
     *
     * @param typeName Name of an interface type matching classes must implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION implement(String typeName);

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doNotImplement(String)}
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    CONJUNCTION dontImplement(String typeName);

    /**
     * Matches classes that do not implement a certain interface with the given type name.
     * This is equivalent to {@link #doNotImplement(Class)}, but does not depend on having a certain
     * type on the classpath.
     *
     * @param typeName Name of an interface type matching classes must not implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotImplement(String typeName);

    /**
     * Matches classes that implement a certain interface matching the given predicate. For example, a call with
     * {@link Predicates#name(String)} would be equivalent to
     * {@link #implement(String)}, but the approach is a lot more generic.
     *
     * @param predicate A predicate identifying interfaces matching classes must implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION implement(DescribedPredicate<? super JavaClass> predicate);

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doNotImplement(DescribedPredicate)}
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    CONJUNCTION dontImplement(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches classes that do not implement a certain interface matching the given predicate.
     * This is the negation of {@link #implement(DescribedPredicate)}.
     *
     * @param predicate A predicate identifying interfaces matching classes must not implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION doNotImplement(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches classes assignable to a certain type (compare {@link Class#isAssignableFrom(Class)} to terminology).
     * A simple example for this predicate would be
     * <pre><code>
     *   assignableTo(Object.class).apply(importedStringClass); // --&gt; returns true
     *   assignableTo(String.class).apply(importedStringClass); // --&gt; returns true
     *   assignableTo(List.class).apply(importedStringClass); // --&gt; returns false
     * </code></pre>
     *
     * @param type An upper type bound to match imported classes against (imported subtypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAssignableTo(Class<?> type);

    /**
     * Matches classes not assignable to a certain type. This is the negation of {@link #areAssignableTo(Class)}.
     *
     * @param type An upper type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAssignableTo(Class<?> type);

    /**
     * Matches classes assignable to a certain type with the given type name. This is equivalent to
     * {@link #areAssignableTo(Class)}, but does not depend on having a certain type on the classpath.
     *
     * @param typeName Name of an upper type bound to match imported classes against (imported subtypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAssignableTo(String typeName);

    /**
     * Matches classes not assignable to a certain type with the given type name.
     * This is equivalent to {@link #areNotAssignableTo(Class)}, but does not depend on having a certain
     * type on the classpath.
     *
     * @param typeName Name of an upper type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAssignableTo(String typeName);

    /**
     * Matches classes assignable to a certain type matching the given predicate. For example, a call with
     * {@link Predicates#name(String)} would be equivalent to
     * {@link #areAssignableTo(String)}, but the approach is a lot more generic.
     *
     * @param predicate A predicate identifying an upper type bound to match imported classes against
     *                  (imported subtypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAssignableTo(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches classes not assignable to a certain type matching the given predicate.
     * This is the negation of {@link #areAssignableTo(DescribedPredicate)}.
     *
     * @param predicate A predicate identifying an upper type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAssignableTo(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches classes assignable from a certain type (compare {@link Class#isAssignableFrom(Class)} to terminology).
     * This is roughly equivalent to the use of reflection:
     * <pre><code>
     *   someClass.class.isAssignableFrom(type);
     * </code></pre>
     * A simple example for this predicate would be
     * <pre><code>
     *   assignableFrom(ArrayList.class).apply(importedArrayListClass); // --&gt; returns true
     *   assignableFrom(ArrayList.class).apply(importedListClass); // --&gt; returns true
     *   assignableFrom(ArrayList.class).apply(importedStringClass); // --&gt; returns false
     * </code></pre>
     *
     * @param type A lower type bound to match imported classes against (imported supertypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAssignableFrom(Class<?> type);

    /**
     * Matches classes not assignable from a certain type. This is the negation of {@link #areAssignableFrom(Class)}.
     *
     * @param type A lower type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAssignableFrom(Class<?> type);

    /**
     * Matches classes assignable from a certain type with the given type name. This is equivalent to
     * {@link #areAssignableFrom(Class)}, but does not depend on having a certain type on the classpath.
     *
     * @param typeName Name of a lower type bound to match imported classes against (imported supertypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAssignableFrom(String typeName);

    /**
     * Matches classes not assignable from a certain type with the given type name.
     * This is equivalent to {@link #areNotAssignableFrom(Class)}, but does not depend on having a certain
     * type on the classpath.
     *
     * @param typeName Name of a lower type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAssignableFrom(String typeName);

    /**
     * Matches classes assignable from a certain type matching the given predicate. For example, a call with
     * {@link Predicates#name(String)} would be equivalent to
     * {@link #areAssignableFrom(String)}, but the approach is a lot more generic.
     *
     * @param predicate A predicate identifying a lower type bound to match imported classes against
     *                  (imported supertypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAssignableFrom(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches classes not assignable from a certain type matching the given predicate.
     * This is the negation of {@link #areAssignableFrom(DescribedPredicate)}.
     *
     * @param predicate A predicate identifying a lower type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAssignableFrom(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches interfaces.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areInterfaces();

    /**
     * Matches everything except interfaces.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotInterfaces();

    /**
     * Matches enums.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areEnums();

    /**
     * Matches everything except enums.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotEnums();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areTopLevelClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotTopLevelClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNestedClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotNestedClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areMemberClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotMemberClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areInnerClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotInnerClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areAnonymousClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotAnonymousClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areLocalClasses();

    @PublicAPI(usage = ACCESS)
    CONJUNCTION areNotLocalClasses();

    /**
     * Matches every class in the supplied list and any of their named/anonymous inner classes,
     * no matter how deeply nested. E.g. consider
     *
     * <pre><code>
     * class Outer {
     *     class Inner {
     *         class EvenMoreInner {
     *         }
     *     }
     * }
     * </code></pre>
     *
     * Then {@link #belongToAnyOf belongToAnyOf(Outer.class)} would match the {@link JavaClass}
     * {@code Outer} but also {@code Inner} and {@code EvenMoreInner}.
     * Likewise would hold for any anonymous inner classes.
     *
     * @param classes List of {@link Class} objects.
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    @PublicAPI(usage = ACCESS)
    CONJUNCTION belongToAnyOf(Class<?>... classes);

}
