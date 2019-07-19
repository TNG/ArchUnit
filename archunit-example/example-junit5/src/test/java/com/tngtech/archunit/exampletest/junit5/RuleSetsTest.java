package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;

@ArchTag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
class RuleSetsTest {
    @ArchTest
    private final ArchRules CODING_RULES = ArchRules.in(CodingRulesTest.class);

    @ArchTest
    private final ArchRules NAMING_CONVENTION_RULES = ArchRules.in(NamingConventionTest.class);
}
