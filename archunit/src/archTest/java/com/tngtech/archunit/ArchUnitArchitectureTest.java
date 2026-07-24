package com.tngtech.archunit;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

import static com.tngtech.archunit.ArchitectureTestImportOptions.DoNotIncludeSelfTests;

@AnalyzeClasses(packages = "com.tngtech.archunit", importOptions = {DoNotIncludeTests.class, DoNotIncludeSelfTests.class})
class ArchUnitArchitectureTest {

    @ArchTest
    static final ArchTests architecture_rules = ArchTests.in(ArchUnitArchitectureRules.class);
}
