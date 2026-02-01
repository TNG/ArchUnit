package com.tngtech.archunit.junit.internal.testexamples;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.junit.internal.testexamples.subone.SimpleRuleField;
import com.tngtech.archunit.junit.internal.testexamples.subtwo.SimpleRules;

@AnalyzeClasses(packages = "some.dummy.package")
public class SimpleRuleLibrary {
    @ArchTest
    public static final ArchTests rules_one = ArchTests.in(SimpleRules.class);

    @ArchTest
    public static final ArchTests rules_two = ArchTests.in(SimpleRuleField.class);

    public static final String RULES_ONE_FIELD = "rules_one";
    public static final String RULES_TWO_FIELD = "rules_two";
}
