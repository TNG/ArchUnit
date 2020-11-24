package com.tngtech.archunit.core.importer.testexamples.simpleimport;

import java.util.List;

@SuppressWarnings("unused")
public @interface AnnotationToImport {
    String someStringMethod() default "DEFAULT";

    Class<?> someTypeMethod() default List.class;

    EnumToImport someEnumMethod() default EnumToImport.SECOND;

    AnnotationParameter someAnnotationMethod() default @AnnotationParameter;
}
