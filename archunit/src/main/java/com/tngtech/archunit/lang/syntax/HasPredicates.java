package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;

interface HasPredicates<T, SELF extends HasPredicates<T, SELF>> {
    SELF with(DescribedPredicate<? super T> predicate);
}
