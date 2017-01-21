package com.tngtech.archunit.lang.syntax.elements;

public interface ClassesShould {
    AccessSpecification access();

    OnlyBeAccessedSpecification<ShouldConjunction> onlyBeAccessed();
}
