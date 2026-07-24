package com.tngtech.archunit.junit;

import com.tngtech.archunit.ArchUnitArchitectureRules;
import com.tngtech.archunit.ArchitectureTestImportOptions.DoNotIncludeSelfTests;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;

@AnalyzeClasses(packages = "com.tngtech.archunit.junit", importOptions = {DoNotIncludeTests.class, DoNotIncludeSelfTests.class})
class ArchUnitArchitectureTest {

    @ArchTest
    static final ArchTests architecture_rules = ArchTests.in(ArchUnitArchitectureRules.class);
}
