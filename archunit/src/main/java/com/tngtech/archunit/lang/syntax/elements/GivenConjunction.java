package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenConjunction<OBJECTS> {
    @PublicAPI(usage = ACCESS)
    ArchRule should(ArchCondition<OBJECTS> condition);

    @PublicAPI(usage = ACCESS)
    GivenConjunction<OBJECTS> and(DescribedPredicate<? super OBJECTS> predicate);
}
