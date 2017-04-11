package com.tngtech.archunit.core.importer.testexamples.annotatedclassimport;

import com.tngtech.archunit.core.importer.testexamples.SomeAnnotation;

import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.SOME_VALUE;

@SomeAnnotation(mandatory = "mandatory", mandatoryEnum = SOME_VALUE)
public class ClassWithUnimportedAnnotation {
}
