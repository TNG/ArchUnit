package com.tngtech.archunit.lang.syntax.elements;

public interface GivenClassesThat extends ClassesThat<GivenClassesThat> {
    ClassesShould should();
}
