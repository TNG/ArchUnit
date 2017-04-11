package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public class ArchPredicates {
    private ArchPredicates() {
    }

    /**
     * This method is just syntactic sugar, e.g. to write aClass.that(is(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> is(DescribedPredicate<T> predicate) {
        return predicate.as("is " + predicate.getDescription());
    }

    /**
     * This method is just syntactic sugar, e.g. to write classes.that(are(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> are(DescribedPredicate<T> predicate) {
        return predicate.as("are " + predicate.getDescription());
    }

    /**
     * This method is just syntactic sugar, e.g. to write method.that(has(type(..))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> has(DescribedPredicate<T> predicate) {
        return predicate.as("has " + predicate.getDescription());
    }

    /**
     * This method is just syntactic sugar, e.g. to write classes.that(have(type(..))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> have(DescribedPredicate<T> predicate) {
        return predicate.as("have " + predicate.getDescription());
    }

    /**
     * This method is just syntactic sugar, e.g. to write classes.should(be(public()))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> be(DescribedPredicate<T> predicate) {
        return predicate.as("be " + predicate.getDescription());
    }
}
