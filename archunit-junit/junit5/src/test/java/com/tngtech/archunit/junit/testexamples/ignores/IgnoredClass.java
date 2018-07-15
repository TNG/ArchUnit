package com.tngtech.archunit.junit.testexamples.ignores;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.testexamples.RuleThatFails;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.junit.testexamples.UnwantedClass.CLASS_VIOLATING_RULES;

@ArchIgnore
@AnalyzeClasses(packages = "some.dummy.package")
public class IgnoredClass {

    @ArchTest
    static final ArchRule rule_one = RuleThatFails.on(CLASS_VIOLATING_RULES);

    @ArchTest
    static void rule_two(JavaClasses classes) {
        RuleThatFails.on(CLASS_VIOLATING_RULES).check(classes);
    }
}
