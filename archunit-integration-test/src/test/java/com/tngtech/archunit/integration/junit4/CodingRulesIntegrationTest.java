package com.tngtech.archunit.integration.junit4;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitIntegrationTestRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class CodingRulesIntegrationTest {

    @ArchTest
    @ExpectedViolationFrom(location = com.tngtech.archunit.integration.CodingRulesIntegrationTest.class,
            method = "expectViolationByWritingToStandardStream")
    public static final ArchRule NO_ACCESS_TO_STANDARD_STREAMS = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    @ExpectedViolationFrom(location = com.tngtech.archunit.integration.CodingRulesIntegrationTest.class,
            method = "expectViolationByThrowingGenericException")
    public static final ArchRule NO_GENERIC_EXCEPTIONS = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    @ExpectedViolationFrom(location = com.tngtech.archunit.integration.CodingRulesIntegrationTest.class,
            method = "expectViolationByUsingJavaUtilLogging")
    public static final ArchRule NO_JAVA_UTIL_LOGGING = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

}
