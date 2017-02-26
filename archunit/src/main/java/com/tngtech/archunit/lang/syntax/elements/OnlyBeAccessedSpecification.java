package com.tngtech.archunit.lang.syntax.elements;

public interface OnlyBeAccessedSpecification<CONJUNCTION> {
    CONJUNCTION byAnyPackage(String... packageIdentifiers);
}
