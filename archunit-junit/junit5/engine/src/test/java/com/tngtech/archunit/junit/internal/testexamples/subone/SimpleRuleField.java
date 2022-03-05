package com.tngtech.archunit.junit.internal.testexamples.subone;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.internal.testexamples.RuleThatFails;
import com.tngtech.archunit.junit.internal.testexamples.UnwantedClass;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "some.dummy.package")
public class SimpleRuleField {
    @ArchTest
    public static final ArchRule simple_rule = RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES);

    public static final String SIMPLE_RULE_FIELD_NAME = "simple_rule";
}
