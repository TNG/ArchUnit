package com.tngtech.archunit.lang.syntax.elements;

import java.lang.annotation.Annotation;

public interface ClassesThat<CONJUNCTION extends Conjunction> {
    CONJUNCTION resideInPackage(String packageIdentifier);

    CONJUNCTION areAnnotatedWith(Class<? extends Annotation> annotationType);
}
