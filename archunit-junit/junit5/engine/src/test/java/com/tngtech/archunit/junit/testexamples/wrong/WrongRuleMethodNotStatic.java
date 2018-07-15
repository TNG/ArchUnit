package com.tngtech.archunit.junit.testexamples.wrong;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

@AnalyzeClasses
public class WrongRuleMethodNotStatic {
    @ArchTest
    void notStatic(JavaClasses classes) {
    }

    public static final String NOT_STATIC_METHOD_NAME = "notStatic";
}
