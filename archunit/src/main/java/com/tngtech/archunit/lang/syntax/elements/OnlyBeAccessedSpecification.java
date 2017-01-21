package com.tngtech.archunit.lang.syntax.elements;

public interface OnlyBeAccessedSpecification<CONJUNCTION extends Conjunction> {
    CONJUNCTION byAnyPackage(String... packageIdentifiers);
}
