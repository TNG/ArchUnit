package com.tngtech.archunit.junit.testexamples;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class SimpleRuleMethod {
    @ArchTest
    public static void simple_rule(JavaClasses classes) {
        classes().should().bePublic().check(classes);
    }

    public static final String SIMPLE_RULE_METHOD_NAME = "simple_rule";
}
