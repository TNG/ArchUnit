package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

public interface GivenObjects<T> {
    ArchRule should(ArchCondition<T> condition);

    GivenObjects<T> as(String description);

    GivenObjects<T> that(DescribedPredicate<? super T> predicate);
}
