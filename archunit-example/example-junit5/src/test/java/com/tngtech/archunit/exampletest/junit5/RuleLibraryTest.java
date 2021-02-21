package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@ArchTag("example")
@AnalyzeClasses(packages = "com.tngtech.archunit.example.layers")
class RuleLibraryTest {
    @ArchTest
    static final ArchTests LIBRARY = ArchTests.in(RuleSetsTest.class);

    @ArchTest
    static final ArchTests FURTHER_CODING_RULES = ArchTests.in(CodingRulesTest.class);

    @ArchTest
    static final ArchTests SLICES_ISOLATION_RULES = ArchTests.in(SlicesIsolationTest.class);
}
