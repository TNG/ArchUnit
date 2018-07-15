package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.testexamples.subone.SimpleRuleField;
import com.tngtech.archunit.junit.testexamples.subtwo.SimpleRules;

@AnalyzeClasses(packages = "some.dummy.package")
public class SimpleRuleLibrary {
    @ArchTest
    public static final ArchRules rules_one = ArchRules.in(SimpleRules.class);

    @ArchTest
    public static final ArchRules rules_two = ArchRules.in(SimpleRuleField.class);

    public static final String RULES_ONE_FIELD = "rules_one";
    public static final String RULES_TWO_FIELD = "rules_two";
}
