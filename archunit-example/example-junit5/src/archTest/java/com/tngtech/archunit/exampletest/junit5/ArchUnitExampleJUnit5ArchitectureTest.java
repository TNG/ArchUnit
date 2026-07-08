package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.ArchUnitExampleArchitectureRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.tngtech.archunit.exampletest.junit5")
class ArchUnitExampleJUnit5ArchitectureTest {

    @ArchTest
    static final ArchTests example_rules = ArchTests.in(ArchUnitExampleArchitectureRules.class);
}
