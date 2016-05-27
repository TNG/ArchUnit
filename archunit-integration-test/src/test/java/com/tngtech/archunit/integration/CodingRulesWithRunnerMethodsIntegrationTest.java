package com.tngtech.archunit.integration;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.exampletest.CodingRulesWithRunnerMethodsTest;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.Rule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static com.tngtech.archunit.integration.CodingRulesIntegrationTest.expectViolationByUsingJavaUtilLogging;

@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class CodingRulesWithRunnerMethodsIntegrationTest extends CodingRulesWithRunnerMethodsTest {
    @Rule
    public final ExpectedViolation expectViolation = ExpectedViolation.none();

    @ArchTest
    @Override
    public void no_java_util_logging_as_method(final JavaClasses classes) {
        expectViolationByUsingJavaUtilLogging(expectViolation);

        expectViolation.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                CodingRulesWithRunnerMethodsIntegrationTest.super.no_java_util_logging_as_method(classes);
            }
        }, Description.createTestDescription(getClass(), "no_java_util_logging_as_method"));
    }
}
