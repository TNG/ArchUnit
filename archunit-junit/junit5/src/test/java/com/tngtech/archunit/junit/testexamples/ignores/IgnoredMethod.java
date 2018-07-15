package com.tngtech.archunit.junit.testexamples.ignores;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.testexamples.RuleThatFails;

import static com.tngtech.archunit.junit.testexamples.UnwantedClass.CLASS_VIOLATING_RULES;

@AnalyzeClasses(packages = "some.dummy.package")
public class IgnoredMethod {

    @ArchTest
    static void unignored_rule(JavaClasses classes) {
        RuleThatFails.on(CLASS_VIOLATING_RULES).check(classes);
    }

    @ArchTest
    @ArchIgnore
    static void ignored_rule(JavaClasses classes) {
        RuleThatFails.on(CLASS_VIOLATING_RULES).check(classes);
    }

    public static final String UNIGNORED_RULE_METHOD = "unignored_rule";
    public static final String IGNORED_RULE_METHOD = "ignored_rule";
}
