package com.tngtech.archunit.junit.internal.testexamples.subone;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.internal.testexamples.RuleThatFails;
import com.tngtech.archunit.junit.internal.testexamples.UnwantedClass;

@AnalyzeClasses(packages = "some.dummy.package")
public class SimpleRuleMethod {
    @ArchTest
    static void simple_rule(JavaClasses classes) {
        RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES).check(classes);
    }

    public static final String SIMPLE_RULE_METHOD_NAME = "simple_rule";
}
