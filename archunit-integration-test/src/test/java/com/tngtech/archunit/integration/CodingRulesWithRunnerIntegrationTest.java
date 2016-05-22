package com.tngtech.archunit.integration;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.junit.AnalyseClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.CLASSES_SHOULD_NOT_USE_JAVA_UTIL_LOGGING;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@RunWith(ArchUnitIntegrationTestRunner.class)
@AnalyseClasses(packages = "com.tngtech.archunit.example")
public class CodingRulesWithRunnerIntegrationTest {

    @ArchTest
    @ExpectedViolationFrom("expectViolationByWritingToStandardStream")
    public static final ArchRule<JavaClass> NO_ACCESS_TO_STANDARD_STREAMS = CLASSES_SHOULD_NOT_ACCESS_STANDARD_STREAMS;

    @ArchTest
    @ExpectedViolationFrom("expectViolationByThrowingGenericException")
    public static final ArchRule<JavaClass> NO_GENERIC_EXCEPTIONS = CLASSES_SHOULD_NOT_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    @ExpectedViolationFrom("expectViolationByUsingJavaUtilLogging")
    public static final ArchRule<JavaClass> NO_JAVA_UTIL_LOGGING = CLASSES_SHOULD_NOT_USE_JAVA_UTIL_LOGGING;

    /**
     * Take the configuration of {@link com.tngtech.archunit.junit.ExpectedViolation} from
     * {@link CodingRulesIntegrationTest}.
     */
    @Retention(RUNTIME)
    @Target(FIELD)
    @interface ExpectedViolationFrom {
        String value();
    }
}
