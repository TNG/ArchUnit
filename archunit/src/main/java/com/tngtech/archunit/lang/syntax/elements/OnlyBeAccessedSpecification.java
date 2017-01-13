package com.tngtech.archunit.lang.syntax.elements;

public interface OnlyBeAccessedSpecification {
    ClassesShould byAnyPackage(String... packageIdentifiers);
}
