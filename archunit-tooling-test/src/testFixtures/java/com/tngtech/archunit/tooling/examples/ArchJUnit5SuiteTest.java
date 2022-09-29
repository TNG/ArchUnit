package com.tngtech.archunit.tooling.examples;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.tngtech.archunit.tooling.examples")
public class ArchJUnit5SuiteTest {

    @ArchTest
    static final ArchTests shouldReportSuccess = ArchTests.in(RuleSuites.ShouldReportSuccessSuite.class);

    @ArchTest
    static final ArchTests shouldReportFailure = ArchTests.in(RuleSuites.ShouldReportFailureSuite.class);

    @ArchTest
    static final ArchTests shouldReportError = ArchTests.in(RuleSuites.ShouldReportErrorSuite.class);

    @ArchTest
    static final ArchTests shouldBeSkipped = ArchTests.in(RuleSuites.ShouldBeSkippedSuite.class);

    @ArchTest
    static final ArchTests shouldBeSkippedConditionally = ArchTests.in(RuleSuites.ShouldBeSkippedConditionallySuite.class);
}
