package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.JavaAnnotation;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaModifier;
import com.tngtech.archunit.core.properties.HasName;

public interface ClassesThat<CONJUNCTION> {
    /**
     * Matches classes by their fully qualified class name.
     *
     * @param name The fully qualified class name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNamed(String name);

    /**
     * Matches classes that don't have a certain fully qualified class name.
     *
     * @param name The fully qualified class name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotNamed(String name);

    /**
     * Matches classes by their simple class name.
     *
     * @param name The simple class name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION haveSimpleName(String name);

    /**
     * Matches classes that don't have a certain simple class name.
     *
     * @param name The simple class name
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION dontHaveSimpleName(String name);

    /**
     * Matches classes with a fully qualified class name matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION haveNameMatching(String regex);

    /**
     * Matches classes with a fully qualified class name not matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION haveNameNotMatching(String regex);

    /**
     * Matches classes residing in a package matching the supplied package identifier.
     *
     * @param packageIdentifier A string identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION resideInAPackage(String packageIdentifier);

    /**
     * Matches classes residing in a package matching any of the supplied package identifiers.
     *
     * @param packageIdentifiers Strings identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION resideInAnyPackage(String... packageIdentifiers);

    /**
     * Matches classes not residing in a package matching the supplied package identifier.
     *
     * @param packageIdentifier A string identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION resideOutsideOfPackage(String packageIdentifier);

    /**
     * Matches classes not residing in a package matching any of the supplied package identifiers.
     *
     * @param packageIdentifiers Strings identifying packages, for details see {@link PackageMatcher}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION resideOutsideOfPackages(String... packageIdentifiers);

    /**
     * Matches public classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION arePublic();

    /**
     * Matches non-public classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotPublic();

    /**
     * Matches protected classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areProtected();

    /**
     * Matches non-protected classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotProtected();

    /**
     * Matches package private classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION arePackagePrivate();

    /**
     * Matches non-package private classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotPackagePrivate();

    /**
     * Matches private classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION arePrivate();

    /**
     * Matches non-private classes.
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotPrivate();

    /**
     * Matches classes having a certain {@link JavaModifier} (e.g. {@link JavaModifier#ABSTRACT}).
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION haveModifier(JavaModifier modifier);

    /**
     * Matches classes not having a certain {@link JavaModifier} (e.g. {@link JavaModifier#ABSTRACT}).
     *
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION dontHaveModifier(JavaModifier modifier);

    /**
     * Matches classes annotated with a certain type of annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Matches classes not annotated with a certain type of annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Matches classes annotated with a certain type of annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areAnnotatedWith(String annotationTypeName);

    /**
     * Matches classes not annotated with a certain type of annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotAnnotatedWith(String annotationTypeName);

    /**
     * Matches classes annotated with a certain annotation, where matching annotations are
     * determined by the supplied predicate.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate);

    /**
     * Matches classes not annotated with a certain annotation, where matching annotations are
     * determined by the supplied predicate.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate);

    /**
     * Matches classes that implement a certain interface.
     *
     * @param type An interface type matching classes must implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION implement(Class<?> type);

    /**
     * Matches classes that don't implement a certain interface. This is the negation of {@link #implement(Class)}.
     *
     * @param type An interface type matching classes must not implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION dontImplement(Class<?> type);

    /**
     * Matches classes that implement a certain interface with the given type name. This is equivalent to
     * {@link #implement(Class)}, but doesn't depend on having a certain type on the classpath.
     *
     * @param typeName Name of an interface type matching classes must implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION implement(String typeName);

    /**
     * Matches classes that don't implement a certain interface with the given type name.
     * This is equivalent to {@link #dontImplement(Class)}, but doesn't depend on having a certain
     * type on the classpath.
     *
     * @param typeName Name of an interface type matching classes must not implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION dontImplement(String typeName);

    /**
     * Matches classes that implement a certain interface matching the given predicate. For example, a call with
     * {@link HasName.Predicates#name(String)} would be equivalent to
     * {@link #implement(String)}, but the approach is a lot more generic.
     *
     * @param predicate A predicate identifying interfaces matching classes must implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION implement(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches classes that don't implement a certain interface matching the given predicate.
     * This is the negation of {@link #implement(DescribedPredicate)}.
     *
     * @param predicate A predicate identifying interfaces matching classes must not implement
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION dontImplement(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches classes assignable to a certain type (compare {@link Class#isAssignableFrom(Class)} to terminology).
     * A simple example for this predicate would be
     * <pre><code>
     *   assignableTo(Object.class).apply(importedStringClass); // -> returns true
     *   assignableTo(String.class).apply(importedStringClass); // -> returns true
     *   assignableTo(List.class).apply(importedStringClass); // -> returns false
     * </code></pre>
     *
     * @param type An upper type bound to match imported classes against (imported subtypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areAssignableTo(Class<?> type);

    /**
     * Matches classes not assignable to a certain type. This is the negation of {@link #areAssignableTo(Class)}.
     *
     * @param type An upper type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotAssignableTo(Class<?> type);

    /**
     * Matches classes assignable to a certain type with the given type name. This is equivalent to
     * {@link #areAssignableTo(Class)}, but doesn't depend on having a certain type on the classpath.
     *
     * @param typeName Name of an upper type bound to match imported classes against (imported subtypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areAssignableTo(String typeName);

    /**
     * Matches classes not assignable to a certain type with the given type name.
     * This is equivalent to {@link #areNotAssignableTo(Class)}, but doesn't depend on having a certain
     * type on the classpath.
     *
     * @param typeName Name of an upper type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotAssignableTo(String typeName);

    /**
     * Matches classes assignable to a certain type matching the given predicate. For example, a call with
     * {@link HasName.Predicates#name(String)} would be equivalent to
     * {@link #areAssignableTo(String)}, but the approach is a lot more generic.
     *
     * @param predicate A predicate identifying an upper type bound to match imported classes against
     *                  (imported subtypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areAssignableTo(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches classes not assignable to a certain type matching the given predicate.
     * This is the negation of {@link #areAssignableTo(DescribedPredicate)}.
     *
     * @param predicate A predicate identifying an upper type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotAssignableTo(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches classes assignable from a certain type (compare {@link Class#isAssignableFrom(Class)} to terminology).
     * This is roughly equivalent to the use of reflection:
     * <pre><code>
     *   someClass.class.isAssignableFrom(type);
     * </code></pre>
     * A simple example for this predicate would be
     * <pre><code>
     *   assignableFrom(ArrayList.class).apply(importedArrayListClass); // -> returns true
     *   assignableFrom(ArrayList.class).apply(importedListClass); // -> returns true
     *   assignableFrom(ArrayList.class).apply(importedStringClass); // -> returns false
     * </code></pre>
     *
     * @param type A lower type bound to match imported classes against (imported supertypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areAssignableFrom(Class<?> type);

    /**
     * Matches classes not assignable from a certain type. This is the negation of {@link #areAssignableFrom(Class)}.
     *
     * @param type A lower type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotAssignableFrom(Class<?> type);

    /**
     * Matches classes assignable from a certain type with the given type name. This is equivalent to
     * {@link #areAssignableFrom(Class)}, but doesn't depend on having a certain type on the classpath.
     *
     * @param typeName Name of a lower type bound to match imported classes against (imported supertypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areAssignableFrom(String typeName);

    /**
     * Matches classes not assignable from a certain type with the given type name.
     * This is equivalent to {@link #areNotAssignableFrom(Class)}, but doesn't depend on having a certain
     * type on the classpath.
     *
     * @param typeName Name of a lower type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotAssignableFrom(String typeName);

    /**
     * Matches classes assignable from a certain type matching the given predicate. For example, a call with
     * {@link HasName.Predicates#name(String)} would be equivalent to
     * {@link #areAssignableFrom(String)}, but the approach is a lot more generic.
     *
     * @param predicate A predicate identifying a lower type bound to match imported classes against
     *                  (imported supertypes will match)
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areAssignableFrom(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches classes not assignable from a certain type matching the given predicate.
     * This is the negation of {@link #areAssignableFrom(DescribedPredicate)}.
     *
     * @param predicate A predicate identifying a lower type bound imported classes should NOT have
     * @return A syntax conjunction element, which can be completed to form a full rule
     */
    CONJUNCTION areNotAssignableFrom(DescribedPredicate<? super JavaClass> predicate);
}
