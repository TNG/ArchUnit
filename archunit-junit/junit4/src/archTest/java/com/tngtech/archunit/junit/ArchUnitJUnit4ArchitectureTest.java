package com.tngtech.archunit.junit;

import com.tngtech.archunit.ArchUnitArchitectureRules;
import com.tngtech.archunit.ArchitectureTestImportOptions.DoNotIncludeSelfTests;

@AnalyzeClasses(packages = "com.tngtech.archunit", importOptions = DoNotIncludeSelfTests.class)
class ArchUnitJUnit4ArchitectureTest {

    @ArchTest
    static final ArchTests architecture_rules = ArchTests.in(ArchUnitArchitectureRules.class);
}
