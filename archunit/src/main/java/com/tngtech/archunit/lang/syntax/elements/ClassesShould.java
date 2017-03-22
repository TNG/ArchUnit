package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.JavaAnnotation;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaMethodCall;
import com.tngtech.archunit.core.JavaModifier;
import com.tngtech.archunit.core.properties.HasName;

public interface ClassesShould {
    /**
     * @return A syntax element that allows restricting how classes should access other (classes/fields/methods/...)
     * <br>E.g.
     * <pre><code>
     *   {@link #access()}.{@link AccessSpecification#classesThat() classesThat()}.{@link ClassesShouldThat#areNamed(String) areNamed(String)}
     * </code></pre>
     */
    AccessSpecification access();

    /**
     * @return A syntax element that allows restricting how classes should be accessed
     * <br>E.g.
     * <pre><code>
     *   {@link #onlyBeAccessed()}.{@link OnlyBeAccessedSpecification#byAnyPackage(String...) byAnyPackage(String...)}
     * </code></pre>
     */
    OnlyBeAccessedSpecification<ClassesShouldConjunction> onlyBeAccessed();

    /**
     * Asserts that classes have a certain fully qualified class name.
     *
     * @param name The fully qualified class name
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beNamed(String name);

    /**
     * Asserts that classes don't have a certain fully qualified class name.
     *
     * @param name The fully qualified class name
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeNamed(String name);

    /**
     * Asserts that classes have a certain simple class name.
     *
     * @param name The simple class name
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction haveSimpleName(String name);

    /**
     * Asserts that classes don't have a certain simple class name.
     *
     * @param name The simple class name
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notHaveSimpleName(String name);

    /**
     * Asserts that classes have a fully qualified class name matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction haveNameMatching(String regex);

    /**
     * Asserts that classes have a fully qualified class name not matching a given regular expression.
     *
     * @param regex A regular expression
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction haveNameNotMatching(String regex);

    /**
     * Asserts that classes reside in a package matching the supplied package identifier.
     *
     * @param packageIdentifier A string identifying packages, for details see {@link PackageMatcher}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction resideInAPackage(String packageIdentifier);

    /**
     * Asserts that classes reside in a package matching any of the supplied package identifiers.
     *
     * @param packageIdentifiers Strings identifying packages, for details see {@link PackageMatcher}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction resideInAnyPackage(String... packageIdentifiers);

    /**
     * Asserts that classes don't reside in a package matching the supplied package identifier.
     *
     * @param packageIdentifier A string identifying packages, for details see {@link PackageMatcher}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction resideOutsideOfPackage(String packageIdentifier);

    /**
     * Asserts that classes don't reside in a package matching any of the supplied package identifiers.
     *
     * @param packageIdentifiers Strings identifying packages, for details see {@link PackageMatcher}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction resideOutsideOfPackages(String... packageIdentifiers);

    /**
     * Asserts that classes are public.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction bePublic();

    /**
     * Asserts that classes are non-public.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBePublic();

    /**
     * Asserts that classes are protected.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beProtected();

    /**
     * Asserts that classes are non-protected.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeProtected();

    /**
     * Asserts that classes are package private.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction bePackagePrivate();

    /**
     * Asserts that classes are non-package private.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBePackagePrivate();

    /**
     * Asserts that classes are private.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction bePrivate();

    /**
     * Asserts that classes are non-private.
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBePrivate();

    /**
     * Asserts that classes have a certain {@link JavaModifier} (e.g. {@link JavaModifier#ABSTRACT}).
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction haveModifier(JavaModifier modifier);

    /**
     * Asserts that classes don't have a certain {@link JavaModifier} (e.g. {@link JavaModifier#ABSTRACT}).
     *
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notHaveModifier(JavaModifier modifier);

    /**
     * Asserts that classes are annotated with a certain type of annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Asserts that classes are not annotated with a certain type of annotation.
     *
     * @param annotationType Specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeAnnotatedWith(Class<? extends Annotation> annotationType);

    /**
     * Asserts that classes are annotated with a certain type of annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beAnnotatedWith(String annotationTypeName);

    /**
     * Asserts that classes are not annotated with a certain type of annotation.
     *
     * @param annotationTypeName Fully qualified class name of a specific type of {@link Annotation}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeAnnotatedWith(String annotationTypeName);

    /**
     * Asserts that classes are annotated with a certain annotation, where matching annotations are
     * determined by the supplied predicate.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate);

    /**
     * Asserts that classes are not annotated with a certain annotation, where matching annotations are
     * determined by the supplied predicate.
     *
     * @param predicate A predicate defining matching {@link JavaAnnotation JavaAnnotations}
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeAnnotatedWith(DescribedPredicate<? super JavaAnnotation> predicate);

    /**
     * Asserts that classes are assignable to a certain type (compare {@link Class#isAssignableFrom(Class)} to terminology).
     * A simple example for this predicate would be
     * <pre><code>
     *   assignableTo(Object.class).apply(importedStringClass); // -> returns true
     *   assignableTo(String.class).apply(importedStringClass); // -> returns true
     *   assignableTo(List.class).apply(importedStringClass); // -> returns false
     * </code></pre>
     *
     * @param type An upper type bound to match imported classes against (imported subtypes will match)
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beAssignableTo(Class<?> type);

    /**
     * Asserts that classes are not assignable to a certain type. This is the negation of {@link #beAssignableTo(Class)}.
     *
     * @param type An upper type bound imported classes should NOT have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeAssignableTo(Class<?> type);

    /**
     * Asserts that classes are assignable to a certain type with the given type name. This is equivalent to
     * {@link #beAssignableTo(Class)}, but doesn't depend on having a certain type on the classpath.
     *
     * @param typeName Name of an upper type bound to match imported classes against (imported subtypes will match)
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beAssignableTo(String typeName);

    /**
     * Asserts that classes are not assignable to a certain type with the given type name.
     * This is equivalent to {@link #notBeAssignableTo(Class)}, but doesn't depend on having a certain
     * type on the classpath.
     *
     * @param typeName Name of an upper type bound imported classes should NOT have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeAssignableTo(String typeName);

    /**
     * Asserts that classes are assignable to a certain type matching the given predicate. For example, a call with
     * {@link HasName.Predicates#name(String)} would be equivalent to
     * {@link #beAssignableTo(String)}, but the approach is a lot more generic.
     *
     * @param predicate A predicate identifying an upper type bound to match imported classes against
     *                  (imported subtypes will match)
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beAssignableTo(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Asserts that classes are not assignable to a certain type matching the given predicate.
     * This is the negation of {@link #beAssignableTo(DescribedPredicate)}.
     *
     * @param predicate A predicate identifying an upper type bound imported classes should NOT have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeAssignableTo(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Asserts that classes are assignable from a certain type (compare {@link Class#isAssignableFrom(Class)} to terminology).
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
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beAssignableFrom(Class<?> type);

    /**
     * Asserts that classes are not assignable from a certain type. This is the negation of {@link #beAssignableFrom(Class)}.
     *
     * @param type A lower type bound imported classes should NOT have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeAssignableFrom(Class<?> type);

    /**
     * Asserts that classes are assignable from a certain type with the given type name. This is equivalent to
     * {@link #beAssignableFrom(Class)}, but doesn't depend on having a certain type on the classpath.
     *
     * @param typeName Name of a lower type bound to match imported classes against (imported supertypes will match)
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beAssignableFrom(String typeName);

    /**
     * Asserts that classes are not assignable from a certain type with the given type name.
     * This is equivalent to {@link #notBeAssignableFrom(Class)}, but doesn't depend on having a certain
     * type on the classpath.
     *
     * @param typeName Name of a lower type bound imported classes should NOT have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeAssignableFrom(String typeName);

    /**
     * Asserts that classes are assignable from a certain type matching the given predicate. For example, a call with
     * {@link HasName.Predicates#name(String)} would be equivalent to
     * {@link #beAssignableFrom(String)}, but the approach is a lot more generic.
     *
     * @param predicate A predicate identifying a lower type bound to match imported classes against
     *                  (imported supertypes will match)
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction beAssignableFrom(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Asserts that classes are not assignable from a certain type matching the given predicate.
     * This is the negation of {@link #beAssignableFrom(DescribedPredicate)}.
     *
     * @param predicate A predicate identifying a lower type bound imported classes should NOT have
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction notBeAssignableFrom(DescribedPredicate<? super JavaClass> predicate);

    /**
     * Matches against all accesses (setting or getting) of a specific field.
     *
     * @param owner     The class declaring the field
     * @param fieldName The name of the field to match
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction accessField(Class<?> owner, String fieldName);

    /**
     * Matches against all accesses (setting or getting) of a specific field.
     *
     * @param ownerName The fully qualified class name of the class declaring the field
     * @param fieldName The name of the field to match
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction accessField(String ownerName, String fieldName);

    /**
     * Matches against accessing fields, where origin (a method or constructor) and target (a field)
     * can be freely restricted by the supplied predicate.
     *
     * @param predicate Determines which {@link JavaFieldAccess JavaFieldAccesses} match the rule
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction accessFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate);

    /**
     * Matches against getting of a specific field (e.g. <code>return someClass.<b>someField</b>;</code>).
     *
     * @param owner     The class declaring the field
     * @param fieldName The name of the field to match
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction getField(Class<?> owner, String fieldName);

    /**
     * Matches against getting a specific field (e.g. <code>return someClass.<b>someField</b>;</code>).
     *
     * @param ownerName The fully qualified class name of the class declaring the field
     * @param fieldName The name of the field to match
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction getField(String ownerName, String fieldName);

    /**
     * Matches against getting of fields, where origin (a method or constructor) and target (a field)
     * can be freely restricted by the supplied predicate.
     *
     * @param predicate Determines which {@link JavaFieldAccess JavaFieldAccesses} match the rule
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction getFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate);

    /**
     * Matches against setting a specific field (e.g. <code>someClass.<b>someField</b> = newValue;</code>).
     *
     * @param owner     The class declaring the field
     * @param fieldName The name of the field to match
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction setField(Class<?> owner, String fieldName);

    /**
     * Matches against setting a specific field (e.g. <code>someClass.<b>someField</b> = newValue;</code>).
     *
     * @param ownerName The fully qualified class name of the class declaring the field
     * @param fieldName The name of the field to match
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction setField(String ownerName, String fieldName);

    /**
     * Matches against setting of fields, where origin (a method or constructor) and target (a field)
     * can be freely restricted by the supplied predicate.
     *
     * @param predicate Determines which {@link JavaFieldAccess JavaFieldAccesses} match the rule
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate);

    /**
     * Matches against a method call to a specific method (e.g. <code>someClass.<b>call()</b>;</code>).
     *
     * @param owner          Class declaring the method
     * @param methodName     The method name to match against
     * @param parameterTypes The parameter types of the respective method
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction callMethod(Class<?> owner, String methodName, Class<?>... parameterTypes);

    /**
     * Matches against method call to a specific method (e.g. <code>someClass.<b>call()</b>;</code>).
     *
     * @param ownerName          The fully qualified class name declaring the method
     * @param methodName         The method name to match against
     * @param parameterTypeNames The fully qualified parameter type names
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction callMethod(String ownerName, String methodName, String... parameterTypeNames);

    /**
     * Matches against method calls where origin (a method or constructor) and target (a method)
     * can be freely restricted by the supplied predicate.
     *
     * @param predicate Determines which {@link JavaMethodCall JavaMethodCalls} match the rule
     * @return A syntax element that can either be used as working rule, or to continue specifying a more complex rule
     */
    ClassesShouldConjunction callMethodWhere(DescribedPredicate<? super JavaMethodCall> predicate);
}
