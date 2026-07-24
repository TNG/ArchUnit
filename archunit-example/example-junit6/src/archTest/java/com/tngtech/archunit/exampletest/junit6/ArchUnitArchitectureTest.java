package com.tngtech.archunit.exampletest.junit6;

import com.tngtech.archunit.ArchUnitExampleArchitectureRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.tngtech.archunit.exampletest.junit6")
class ArchUnitArchitectureTest {

    @ArchTest
    static final ArchTests example_rules = ArchTests.in(ArchUnitExampleArchitectureRules.class);
}
