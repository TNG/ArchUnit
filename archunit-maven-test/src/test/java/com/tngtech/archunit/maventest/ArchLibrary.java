package com.tngtech.archunit.maventest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.maventest.ArchSubLibrary.registerCallAs;

public class ArchLibrary {
    static final String RULE_ON_LEVEL_ONE_DESCRIPTOR = "rule_on_level_one";
    static final String RULE_METHOD_ON_LEVEL_ONE_DESCRIPTOR = "rule_method_on_level_one";

    @ArchTest
    public static final ArchRule rule_on_level_one =
            classes().should(registerCallAs(ArchLibrary.class, RULE_ON_LEVEL_ONE_DESCRIPTOR));

    @ArchTest
    public static void rule_method_on_level_one(JavaClasses classes) {
        CalledRuleRecords.register(ArchLibrary.class, RULE_METHOD_ON_LEVEL_ONE_DESCRIPTOR);
    }

    @ArchTest
    public static final ArchRules sub_library = ArchRules.in(ArchSubLibrary.class);
}
