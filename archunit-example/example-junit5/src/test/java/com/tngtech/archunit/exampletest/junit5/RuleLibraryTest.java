package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;

@ArchTag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
class RuleLibraryTest {
    @ArchTest
    static final ArchRules LIBRARY = ArchRules.in(RuleSetsTest.class);

    @ArchTest
    static final ArchRules FURTHER_CODING_RULES = ArchRules.in(CodingRulesTest.class);

    @ArchTest
    static final ArchRules SLICES_ISOLATION_RULES = ArchRules.in(SlicesIsolationTest.class);
}
