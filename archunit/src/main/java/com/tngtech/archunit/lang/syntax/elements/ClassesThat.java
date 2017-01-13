package com.tngtech.archunit.lang.syntax.elements;

public interface ClassesThat<SELF extends ClassesThat<SELF>> {
    SELF resideInPackage(String packageIdentifier);
}
