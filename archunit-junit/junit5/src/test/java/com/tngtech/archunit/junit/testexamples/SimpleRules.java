package com.tngtech.archunit.junit.testexamples;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class SimpleRules {
    @ArchTest
    public static final ArchRule simple_rule_field_one = classes().should().bePublic();

    @ArchTest
    public static final ArchRule simple_rule_field_two = classes().should().bePublic();

    @ArchTest
    public static void simple_rule_method_one(JavaClasses classes) {
        classes().should().bePublic().check(classes);
    }

    @ArchTest
    public static void simple_rule_method_two(JavaClasses classes) {
        classes().should().bePublic().check(classes);
    }

    public static final Set<String> RULE_FIELD_NAMES = ImmutableSet.of("simple_rule_field_one", "simple_rule_field_two");

    public static final Set<String> RULE_METHOD_NAMES = ImmutableSet.of("simple_rule_method_one", "simple_rule_method_two");
}
