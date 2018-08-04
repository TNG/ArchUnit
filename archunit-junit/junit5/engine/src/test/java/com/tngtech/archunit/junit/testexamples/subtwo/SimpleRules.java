package com.tngtech.archunit.junit.testexamples.subtwo;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.testexamples.RuleThatFails;
import com.tngtech.archunit.junit.testexamples.UnwantedClass;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "some.dummy.package")
public class SimpleRules {
    @ArchTest
    public static final ArchRule simple_rule_field_one = RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES);

    @ArchTest
    public static final ArchRule simple_rule_field_two = RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES);

    @ArchTest
    public static void simple_rule_method_one(JavaClasses classes) {
        RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES).check(classes);
    }

    @ArchTest
    public static void simple_rule_method_two(JavaClasses classes) {
        RuleThatFails.on(UnwantedClass.CLASS_VIOLATING_RULES).check(classes);
    }

    public static final String SIMPLE_RULE_FIELD_ONE_NAME = "simple_rule_field_one";
    public static final String SIMPLE_RULE_FIELD_TWO_NAME = "simple_rule_field_two";
    public static final Set<String> RULE_FIELD_NAMES = ImmutableSet.of(SIMPLE_RULE_FIELD_ONE_NAME, SIMPLE_RULE_FIELD_TWO_NAME);
    public static final String SIMPLE_RULE_METHOD_ONE_NAME = "simple_rule_method_one";
    public static final Set<String> RULE_METHOD_NAMES = ImmutableSet.of(SIMPLE_RULE_METHOD_ONE_NAME, "simple_rule_method_two");
}
