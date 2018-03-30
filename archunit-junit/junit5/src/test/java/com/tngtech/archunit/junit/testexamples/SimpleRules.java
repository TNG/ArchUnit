package com.tngtech.archunit.junit.testexamples;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

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

    public static final Set<String> RULE_FIELD_NAMES = ImmutableSet.of("simple_rule_field_one", "simple_rule_field_two");
    public static final Set<String> RULE_METHOD_NAMES = ImmutableSet.of("simple_rule_method_one", "simple_rule_method_two");
}
