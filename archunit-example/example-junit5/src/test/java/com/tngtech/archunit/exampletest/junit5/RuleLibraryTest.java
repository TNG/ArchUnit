package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchRules;
import com.tngtech.archunit.junit.ArchTest;
import org.junit.jupiter.api.Tag;

@Tag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
class RuleLibraryTest {
    @ArchTest
    static final ArchRules LIBRARY = ArchRules.in(RuleSetsTest.class);

    @ArchTest
    static final ArchRules FURTHER_CODING_RULES = ArchRules.in(CodingRulesMethodsTest.class);

    @ArchTest
    static final ArchRules SLICES_ISOLATION_RULES = ArchRules.in(SlicesIsolationTest.class);
}
