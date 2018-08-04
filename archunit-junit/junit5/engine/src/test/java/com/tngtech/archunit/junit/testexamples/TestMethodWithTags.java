package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;

@AnalyzeClasses
public class TestMethodWithTags {
    public static final String METHOD_WITH_TAG_NAME = "method_with_tag";

    @ArchTag("method-tag-one")
    @ArchTag("method-tag-two")
    @ArchTest
    static void method_with_tag(JavaClasses classes) {
    }
}
