package com.tngtech.archunit.exampletest;

import com.tngtech.archunit.ArchUnitExampleArchitectureRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(
        packages = {"com.tngtech.archunit.example", "com.tngtech.archunit.exampletest"})
public class ArchUnitArchitectureTest {
    private ArchUnitArchitectureTest() {
    }

    @ArchTest
    public static final ArchTests example_rules = ArchTests.in(ArchUnitExampleArchitectureRules.class);
}
