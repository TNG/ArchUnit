package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.base.DescribedPredicate;

public class ArchPredicates {
    private ArchPredicates() {
    }

    public static CallPredicate callTarget() {
        return CallPredicate.target();
    }

    public static CallPredicate callOrigin() {
        return CallPredicate.origin();
    }

    /**
     * This method is just syntactic sugar, e.g. to write aClass.that(is(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate with adjusted description
     */
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
    public static <T> DescribedPredicate<T> have(DescribedPredicate<T> predicate) {
        return predicate.as("have " + predicate.getDescription());
    }
}
