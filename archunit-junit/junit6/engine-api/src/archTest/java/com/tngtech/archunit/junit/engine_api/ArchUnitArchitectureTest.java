package com.tngtech.archunit.junit.engine_api;

import com.tngtech.archunit.ArchUnitArchitectureRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.tngtech.archunit.junit.engine_api")
class ArchUnitArchitectureTest {

    @ArchTest
    static final ArchTests architecture_rules = ArchTests.in(ArchUnitArchitectureRules.class);
}
