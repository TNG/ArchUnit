package com.tngtech.archunit.exampletest.junit5;

import com.tngtech.archunit.ArchUnitExampleArchitectureRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.tngtech.archunit.exampletest.junit5")
public class ArchUnitArchitectureTest {
    private ArchUnitArchitectureTest() {
    }

    @ArchTest
    public static final ArchTests example_rules = ArchTests.in(ArchUnitExampleArchitectureRules.class);
}
