package com.tngtech.archunit.junit;

import com.tngtech.archunit.ArchitectureTestImportOptions.DoNotIncludeSelfTests;
import com.tngtech.archunit.PublicAPIRules;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;

@AnalyzeClasses(packages = "com.tngtech.archunit.junit", importOptions = {DoNotIncludeTests.class, DoNotIncludeSelfTests.class})
public class ArchUnitArchitectureTest {
    private ArchUnitArchitectureTest() {
    }

    @ArchTest
    public static final ArchTests public_api_rules = ArchTests.in(PublicAPIRules.class);
}
