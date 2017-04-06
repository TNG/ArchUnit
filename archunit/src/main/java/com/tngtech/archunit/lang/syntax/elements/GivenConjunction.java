package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

public interface GivenConjunction<OBJECTS> {
    ArchRule should(ArchCondition<OBJECTS> condition);
}
