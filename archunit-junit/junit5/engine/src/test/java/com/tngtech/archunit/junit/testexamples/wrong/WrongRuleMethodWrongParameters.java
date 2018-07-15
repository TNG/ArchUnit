package com.tngtech.archunit.junit.testexamples.wrong;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

@AnalyzeClasses
public class WrongRuleMethodWrongParameters {
    @ArchTest
    static void wrongParameters() {
    }

    public static final String WRONG_PARAMETERS_METHOD_NAME = "wrongParameters";
}
