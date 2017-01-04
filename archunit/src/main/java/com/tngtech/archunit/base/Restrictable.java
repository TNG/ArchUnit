package com.tngtech.archunit.base;

/**
 * Common Interface for types that allow a restriction by predicates,<br>
 * e.g. classes.that(areServices) -> subset of classes that match the predicate and thus represent services
 *
 * @param <TYPE> The type of elements of this collection
 * @param <SELF> The type of the concrete collection implementation itself
 */
public interface Restrictable<TYPE, SELF extends Iterable<TYPE>> extends Iterable<TYPE> {
    /**
     * Limits this iterable to a iterable of the same type holding just the elements, that match the predicate.
     *
     * @param predicate The predicate elements of the result must satisfy, optionally a description for the result
     * @return A iterable of the same type holding only elements satisfying the predicate, and optionally with
     * a description set by the predicate
     */
    SELF that(DescribedPredicate<? super TYPE> predicate);
}
