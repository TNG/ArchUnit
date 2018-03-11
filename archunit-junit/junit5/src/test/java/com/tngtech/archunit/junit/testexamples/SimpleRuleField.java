package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class SimpleRuleField {
    @ArchTest
    public static final ArchRule simple_rule = classes().should().bePublic();

    public static final String SIMPLE_RULE_FIELD_NAME = "simple_rule";
}
