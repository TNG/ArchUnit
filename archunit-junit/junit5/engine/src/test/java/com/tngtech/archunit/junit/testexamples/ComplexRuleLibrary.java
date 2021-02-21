package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.junit.testexamples.subtwo.SimpleRules;

@AnalyzeClasses(packages = "some.dummy.package")
public class ComplexRuleLibrary {
    @ArchTest
    public static final ArchTests rules_one = ArchTests.in(SimpleRuleLibrary.class);

    @ArchTest
    public static final ArchTests rules_two = ArchTests.in(SimpleRules.class);

    public static final String RULES_ONE_FIELD = "rules_one";
    public static final String RULES_TWO_FIELD = "rules_two";
}
