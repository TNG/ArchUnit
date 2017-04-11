package com.tngtech.archunit.core.importer.testexamples.annotatedclassimport;

import java.io.Serializable;

import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.OTHER_VALUE;
import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.SOME_VALUE;

@SimpleAnnotation("some")
@TypeAnnotationWithEnumAndArrayValue(
        value = OTHER_VALUE,
        enumArray = {SOME_VALUE, OTHER_VALUE},
        subAnnotation = @SimpleAnnotation("sub"),
        subAnnotationArray = {@SimpleAnnotation("otherFirst"), @SimpleAnnotation("otherSecond")},
        clazz = Serializable.class,
        classes = {Serializable.class, String.class}
)
public class ClassWithComplexAnnotations {
}
