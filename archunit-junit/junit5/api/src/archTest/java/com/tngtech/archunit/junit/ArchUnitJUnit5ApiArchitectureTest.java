package com.tngtech.archunit.junit;

import com.tngtech.archunit.ArchUnitArchitectureRules;
import com.tngtech.archunit.ArchitectureTestImportOptions.DoNotIncludeSelfTests;
import com.tngtech.archunit.core.domain.JavaClasses;

@AnalyzeClasses(packages = "com.tngtech.archunit", importOptions = DoNotIncludeSelfTests.class)
class ArchUnitJUnit5ApiArchitectureTest {

    @ArchTest
    static final ArchTests architecture_rules = ArchTests.in(ArchUnitArchitectureRules.class);

    /**
     * If we accidentally have a newer archunit-junit* version on the classpath, the testee will be replaced
     * and the test will not run on the correct target.
     */
    @ArchTest
    static void testee_is_archunit_junit5_api(JavaClasses classes) {
        String actualSource = ArchTest.class.getProtectionDomain().getCodeSource().getLocation().toString();
        if (!actualSource.contains("archunit-junit5-api")) {
            throw new AssertionError("expected archunit-junit5-api in path " + actualSource);
        }
    }
}
