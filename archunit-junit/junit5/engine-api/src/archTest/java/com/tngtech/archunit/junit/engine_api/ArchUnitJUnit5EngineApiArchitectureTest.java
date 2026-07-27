package com.tngtech.archunit.junit.engine_api;

import com.tngtech.archunit.ArchUnitArchitectureRules;
import com.tngtech.archunit.ArchitectureTestImportOptions.DoNotIncludeSelfTests;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.tngtech.archunit", importOptions = DoNotIncludeSelfTests.class)
class ArchUnitJUnit5EngineApiArchitectureTest {

    @ArchTest
    static final ArchTests architecture_rules = ArchTests.in(ArchUnitArchitectureRules.class);

    /**
     * If we accidentally have a newer archunit-junit* version on the classpath, the testee will be replaced
     * and the test will not run on the correct target.
     */
    @ArchTest
    static void testee_is_archunit_junit5_engine_api(JavaClasses classes) {
        String actualSource = FieldSelector.class.getProtectionDomain().getCodeSource().getLocation().toString();
        if (!actualSource.contains("archunit-junit5-engine-api")) {
            throw new AssertionError("expected archunit-junit5-engine-api in path " + actualSource);
        }
    }
}
