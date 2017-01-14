package com.tngtech.archunit.core.testexamples.annotatedclassimport;

import com.tngtech.archunit.core.testexamples.SomeAnnotation;

import static com.tngtech.archunit.core.testexamples.SomeEnum.SOME_VALUE;

@SomeAnnotation(mandatory = "mandatory", mandatoryEnum = SOME_VALUE)
public class ClassWithUnimportedAnnotation {
}
