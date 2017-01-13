package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.lang.ArchRule;

public interface ClassesShould extends ArchRule {
    AccessSpecification access();

    OnlyBeAccessedSpecification onlyBeAccessed();
}
