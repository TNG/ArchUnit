package com.tngtech.archunit.exampletest.junit4;

import com.tngtech.archunit.ArchUnitExampleArchitectureRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.tngtech.archunit.exampletest.junit4")
class ArchUnitExampleJUnit4ArchitectureTest {

    @ArchTest
    static final ArchTests example_rules = ArchTests.in(ArchUnitExampleArchitectureRules.class);
}
