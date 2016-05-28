package com.tngtech.archunit.integration;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_USE_JAVA_UTIL_LOGGING;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class CodingRulesWithRunnerIntegrationTest {

    @ArchTest
    @ExpectedViolationFrom(location = CodingRulesIntegrationTest.class, method = "expectViolationByWritingToStandardStream")
    public static final ArchRule<JavaClass> NO_ACCESS_TO_STANDARD_STREAMS = CLASSES_SHOULD_NOT_ACCESS_STANDARD_STREAMS;

    @ArchTest
    @ExpectedViolationFrom(location = CodingRulesIntegrationTest.class, method = "expectViolationByThrowingGenericException")
    public static final ArchRule<JavaClass> NO_GENERIC_EXCEPTIONS = CLASSES_SHOULD_NOT_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    @ExpectedViolationFrom(location = CodingRulesIntegrationTest.class, method = "expectViolationByUsingJavaUtilLogging")
    public static final ArchRule<JavaClass> NO_JAVA_UTIL_LOGGING = CLASSES_SHOULD_NOT_USE_JAVA_UTIL_LOGGING;

}
