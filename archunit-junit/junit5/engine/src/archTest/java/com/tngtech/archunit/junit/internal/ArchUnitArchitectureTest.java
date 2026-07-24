package com.tngtech.archunit.junit.internal;

import com.tngtech.archunit.ArchUnitArchitectureRules;
import com.tngtech.archunit.ArchitectureTestImportOptions.DoNotIncludeSelfTests;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.tngtech.archunit.junit.internal", importOptions = {DoNotIncludeTests.class, DoNotIncludeSelfTests.class})
class ArchUnitArchitectureTest {

    @ArchTest
    static final ArchTests architecture_rules = ArchTests.in(ArchUnitArchitectureRules.class);
}
